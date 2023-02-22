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

package com.tencent.polaris.grpc.ratelimit;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetServiceRuleRequest;
import com.tencent.polaris.api.rpc.ServiceRuleResponse;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.grpc.interceptor.PolarisServerInterceptor;
import com.tencent.polaris.grpc.util.Common;
import com.tencent.polaris.grpc.util.PolarisHelper;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.Argument;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import com.tencent.polaris.ratelimit.factory.LimitAPIFactory;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.MatchArgument;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.RateLimit;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Rule;
import io.grpc.Grpc;
import io.grpc.HttpConnectProxiedSocketAddress;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.Status;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC-Server 端限流拦截器
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisRateLimitServerInterceptor extends PolarisServerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(PolarisRateLimitServerInterceptor.class);

    private LimitAPI limitAPI;

    private ConsumerAPI consumerAPI;

    private String namespace = "default";

    private String applicationName = "";

    private BiFunction<QuotaResponse, String, Status> rateLimitCallback;

    public PolarisRateLimitServerInterceptor() {
    }

    public void setRateLimitCallback(BiFunction<QuotaResponse, String, Status> rateLimitCallback) {
        this.rateLimitCallback = rateLimitCallback;
    }

    @Override
    public void init(final String namespace, final String applicationName, SDKContext context) {
        this.namespace = namespace;
        this.applicationName = applicationName;
        this.limitAPI = LimitAPIFactory.createLimitAPIByContext(context);
        this.consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(context);
    }

    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                      ServerCallHandler<ReqT, RespT> next) {
        final String applicationName = this.applicationName;
        final boolean applicationRegisterMode = StringUtils.isNotBlank(applicationName);
        final String serviceName = applicationRegisterMode ?  applicationName :
                call.getMethodDescriptor().getServiceName();
        final String method = applicationRegisterMode ? call.getMethodDescriptor().getFullMethodName()
                : call.getMethodDescriptor().getBareMethodName();

        final QuotaRequest request = new QuotaRequest();
        request.setNamespace(namespace);
        request.setService(serviceName);
        request.setMethod(method);
        request.setCount(1);

        RateLimitResp ruleResp = loadRateLimitRule(new ServiceKey(namespace, serviceName));
        request.setArguments(buildArguments(ruleResp, call, headers));

        LOG.debug("[grpc-polaris] do acquire rate-limit quota, request : {}", request);

        final QuotaResponse response = limitAPI.getQuota(request);
        if (Objects.equals(response.getCode(), QuotaResultCode.QuotaResultOk)) {
            return next.startCall(call, headers);
        }

        Status errStatus = rateLimitCallback.apply(response, call.getMethodDescriptor().getFullMethodName());
        call.close(errStatus, headers);
        return new ServerCall.Listener<ReqT>() {
        };
    }

    private <ReqT, RespT> Set<Argument> buildArguments(RateLimitResp rateLimitResp,
                                                       ServerCall<ReqT, RespT> call, Metadata headers) {
        final Set<Argument> arguments = new HashSet<>();
        final Set<MatchArgument> matchArguments = new HashSet<>();

        rateLimitResp.rules.forEach(rule -> {
            if (rule.hasDisable()) {
                return;
            }
            matchArguments.addAll(rule.getArgumentsList());
        });

        matchArguments.forEach(argument -> {
            String key = argument.getKey();
            switch (argument.getType()) {
                case HEADER:
                    arguments.add(Argument.buildHeader(argument.getKey(), headers.get(Key.of(key, Metadata.ASCII_STRING_MARSHALLER))));
                    break;
                case CALLER_IP:
                    HttpConnectProxiedSocketAddress clientAddress = (HttpConnectProxiedSocketAddress) call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
                    if (Objects.nonNull(clientAddress)) {
                        InetSocketAddress address = clientAddress.getTargetAddress();
                        if (Objects.nonNull(address)) {
                            arguments.add(Argument.buildCallerIP(address.getAddress().getHostAddress()));
                        }
                    }
                    break;
                case CALLER_SERVICE:
                    String callerNamespace = headers.get(Common.CALLER_NAMESPACE_KEY);
                    String callerService = headers.get(Common.CALLER_SERVICE_KEY);
                    arguments.add(Argument.buildCallerService(callerNamespace, callerService));
                    break;
            }
        });

        Set<Argument> finalArguments = PolarisHelper.getLabelsInject().modifyRateLimit(arguments);
        return finalArguments;
    }

    private RateLimitResp loadRateLimitRule(ServiceKey target) {
        GetServiceRuleRequest inBoundReq = new GetServiceRuleRequest();
        inBoundReq.setService(target.getService());
        inBoundReq.setNamespace(target.getNamespace());
        inBoundReq.setRuleType(EventType.RATE_LIMITING);

        ServiceRuleResponse inBoundResp = consumerAPI.getServiceRule(inBoundReq);
        RateLimit inBoundRule = (RateLimit) inBoundResp.getServiceRule().getRule();
        if (Objects.nonNull(inBoundRule)) {
            return new RateLimitResp(inBoundRule.getRulesList(), target);
        }
        return new RateLimitResp(Collections.emptyList(), null);
    }

    private static class RateLimitResp {
        final List<Rule> rules;
        final ServiceKey serviceKey;

        private RateLimitResp(List<Rule> rules, ServiceKey serviceKey) {
            this.rules = rules;
            this.serviceKey = serviceKey;
        }

    }

}
