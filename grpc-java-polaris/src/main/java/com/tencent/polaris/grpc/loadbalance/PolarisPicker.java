/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.tencent.polaris.grpc.loadbalance;

import com.google.common.base.Preconditions;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.SourceService;
import com.tencent.polaris.api.rpc.GetServiceRuleRequest;
import com.tencent.polaris.api.rpc.ServiceRuleResponse;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.grpc.util.ClientCallInfo;
import com.tencent.polaris.grpc.util.Common;
import com.tencent.polaris.grpc.util.PolarisHelper;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceResponse;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto.Route;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto.Routing;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto.Source;
import io.grpc.Attributes;
import io.grpc.Context;
import io.grpc.LoadBalancer.PickResult;
import io.grpc.LoadBalancer.PickSubchannelArgs;
import io.grpc.LoadBalancer.Subchannel;
import io.grpc.LoadBalancer.SubchannelPicker;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.Status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tencent.polaris.api.utils.RuleUtils.MATCH_ALL;

/**
 * The main balancing logic.  It <strong>must be thread-safe</strong>. Typically it should only
 * synchronize on its own state, and avoid synchronizing with the LoadBalancer's state.
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisPicker extends SubchannelPicker {

    private static final Logger LOG = LoggerFactory.getLogger(PolarisPicker.class);

    private final Map<PolarisSubChannel, PolarisSubChannel> channels;

    private final SDKContext context;

    private final ConsumerAPI consumerAPI;

    private final Attributes attributes;

    private final ServiceKey sourceService;

    private final RouterAPI routerAPI;

    public PolarisPicker(final Map<PolarisSubChannel, PolarisSubChannel> channels,
                         final SDKContext context,
                         final ConsumerAPI consumerAPI,
                         final RouterAPI routerAPI,
                         final ServiceKey sourceService,
                         final Attributes attributes) {
        this.context = context;
        this.channels = channels;
        this.consumerAPI = consumerAPI;
        this.routerAPI = routerAPI;
        this.attributes = attributes;
        this.sourceService = sourceService;
    }

    @Override
    public PickResult pickSubchannel(PickSubchannelArgs args) {
        if (channels.isEmpty()) {
            return PickResult.withNoResult();
        }

        final String targetNamespace = attributes.get(Common.TARGET_NAMESPACE_KEY);
        final String targetService = attributes.get(Common.TARGET_SERVICE_KEY);
        final ServiceKey target = new ServiceKey(targetNamespace, targetService);

        List<Instance> instances = new ArrayList<>();
        channels.forEach((key, val) -> instances.add(val));

        ServiceInstances serviceInstances = new DefaultServiceInstances(target, instances);

        try {
            Instance instance = doLoadBalance(doRoute(serviceInstances, target, args));
            Subchannel channel = channels.get(new PolarisSubChannel(instance));

            if (Objects.isNull(channel)) {
                return PickResult.withNoResult();
            }

            return PickResult.withSubchannel(channel,
                    new PolarisClientStreamTracerFactory(ClientCallInfo.builder()
                            .consumerAPI(consumerAPI)
                            .instance(instance)
                            .targetNamespace(targetNamespace)
                            .targetService(targetService)
                            .method(args.getMethodDescriptor().getBareMethodName())
                            .build()));
        } catch (PolarisException e) {
            LOG.error("[grpc-polaris] pick subChannel fail", e);
            return PickResult.withError(Status.UNKNOWN.withCause(e));
        }
    }

    Instance doLoadBalance(ServiceInstances serviceInstances) {
        if (serviceInstances.getInstances().size() == 1) {
            return serviceInstances.getInstances().get(0);
        }

        ProcessLoadBalanceRequest request = new ProcessLoadBalanceRequest();
        request.setDstInstances(serviceInstances);

        ProcessLoadBalanceResponse response = routerAPI.processLoadBalance(request);
        return response.getTargetInstance();
    }

    ServiceInstances doRoute(ServiceInstances serviceInstances, ServiceKey target, PickSubchannelArgs args) {
        ProcessRoutersRequest request = new ProcessRoutersRequest();
        request.setDstInstances(serviceInstances);

        final SourceService serviceInfo = new SourceService();
        ServiceKey source = null;
        if (Objects.nonNull(sourceService)) {
            source = new ServiceKey(sourceService.getNamespace(), sourceService.getService());
            serviceInfo.setNamespace(sourceService.getNamespace());
            serviceInfo.setService(sourceService.getService());
        }

        serviceInfo.setArguments(collectRoutingLabels(loadRouteRule(target, source), args));
        request.setSourceService(serviceInfo);

        ProcessRoutersResponse response = routerAPI.processRouters(request);

        return response.getServiceInstances();
    }

    private Set<RouteArgument> collectRoutingLabels(List<Route> routes, PickSubchannelArgs args) {
        Set<String> labelKeys = new HashSet<>();
        routes.forEach(route -> {
            for (Source source : route.getSourcesList()) {
                labelKeys.addAll(source.getMetadataMap().keySet());
            }
        });

        final Set<RouteArgument> arguments = new HashSet<>();
        final Metadata headers = args.getHeaders();

        labelKeys.forEach(labelKey -> {
            if (StringUtils.equals(labelKey, RouteArgument.LABEL_KEY_PATH)) {
                arguments.add(RouteArgument.buildPath(args.getMethodDescriptor().getFullMethodName()));
                return;
            }
            if (labelKey.startsWith(RouteArgument.LABEL_KEY_HEADER)) {
                String headerKey = labelKey.substring(RouteArgument.LABEL_KEY_HEADER.length() + 1);
                arguments.add(RouteArgument.buildHeader(headerKey, headers.get(Key.of(headerKey, Metadata.ASCII_STRING_MARSHALLER))));
                return;
            }
            if (labelKey.startsWith(RouteArgument.LABEL_KEY_CALLER_IP)) {
                arguments.add(RouteArgument.buildCallerIP(context.getConfig().getGlobal().getAPI().getBindIP()));
            }
        });

        return PolarisHelper.getLabelsInject().modifyRoute(new HashSet<>(arguments));
    }

    private List<Route> loadRouteRule(ServiceKey target, ServiceKey source) {
        List<Route> rules = new ArrayList<>();

        GetServiceRuleRequest inBoundReq = new GetServiceRuleRequest();
        inBoundReq.setService(target.getService());
        inBoundReq.setNamespace(target.getNamespace());
        inBoundReq.setRuleType(EventType.ROUTING);

        ServiceRuleResponse inBoundResp = consumerAPI.getServiceRule(inBoundReq);
        Routing inBoundRule = (Routing) inBoundResp.getServiceRule().getRule();
        if (Objects.nonNull(inBoundRule)) {
            List<Route> tmpRules = new RouteResp(inBoundRule.getInboundsList(), target).doFilter();
            rules.addAll(tmpRules);
        }

        if (Objects.isNull(source)) {
            return rules;
        }

        GetServiceRuleRequest outBoundReq = new GetServiceRuleRequest();
        outBoundReq.setService(source.getService());
        outBoundReq.setNamespace(source.getNamespace());
        outBoundReq.setRuleType(EventType.ROUTING);

        ServiceRuleResponse outBoundResp = consumerAPI.getServiceRule(outBoundReq);
        Routing outBoundRule = (Routing) outBoundResp.getServiceRule().getRule();
        if (Objects.nonNull(outBoundRule)) {
            List<Route> tmpRules = new RouteResp(outBoundRule.getOutboundsList(), source).doFilter();
            rules.addAll(tmpRules);
        }
        return rules;
    }

    public static final class EmptyPicker extends SubchannelPicker {

        private final Status status;

        EmptyPicker(Status status) {
            this.status = Preconditions.checkNotNull(status, "status");
        }

        public PickResult pickSubchannel(PickSubchannelArgs args) {
            return this.status.isOk() ? PickResult.withNoResult() : PickResult.withError(this.status);
        }
    }

    private static class RouteResp {

        final List<Route> rule;
        final ServiceKey serviceKey;

        private RouteResp(List<Route> rule, ServiceKey serviceKey) {
            this.rule = rule;
            this.serviceKey = serviceKey;
        }


        List<Route> doFilter() {
            List<Route> newRule = rule.stream().filter((Predicate<Route>) route -> {
                for (Source source : route.getSourcesList()) {

                    if (Objects.equals(source.getNamespace().getValue(), MATCH_ALL) &&
                            Objects.equals(source.getService().getValue(), MATCH_ALL)) {
                        return true;
                    }

                    if (Objects.equals(source.getNamespace().getValue(), MATCH_ALL) &&
                            Objects.equals(source.getService().getValue(), serviceKey.getService())) {
                        return true;
                    }

                    if (Objects.equals(source.getNamespace().getValue(), serviceKey.getNamespace()) &&
                            Objects.equals(source.getService().getValue(), serviceKey.getService())) {
                        return true;
                    }
                }

                return false;
            }).collect(Collectors.toList());

            return newRule;
        }
    }
}
