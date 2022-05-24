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
import com.tencent.polaris.client.pb.RateLimitProto.RateLimit;
import com.tencent.polaris.client.pb.RateLimitProto.Rule;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.grpc.interceptor.PolarisServerInterceptor;
import com.tencent.polaris.grpc.util.PolarisHelper;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import com.tencent.polaris.ratelimit.factory.LimitAPIFactory;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.Status;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

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
        final String serviceName = StringUtils.isBlank(applicationName) ? call.getMethodDescriptor().getServiceName() : applicationName;
        final String method = call.getMethodDescriptor().getBareMethodName();

        final QuotaRequest request = new QuotaRequest();
        request.setNamespace(namespace);
        request.setService(serviceName);
        request.setMethod(method);
        request.setCount(1);

        final Map<String, String> labels = collectLabels(loadRateLimitRule(new ServiceKey(namespace, serviceName)), headers);
        request.setLabels(labels);

        LOG.debug("[grpc-polaris] do acquire rate-limit quota, request : {}", request);

        final QuotaResponse response = limitAPI.getQuota(request);
        if (Objects.equals(response.getCode(), QuotaResultCode.QuotaResultOk)) {
            return next.startCall(call, headers);
        }

        Status errStatus = rateLimitCallback.apply(response, call.getMethodDescriptor().getFullMethodName());
        call.close(errStatus, headers);
        return new ServerCall.Listener<ReqT>() {};
    }

    private Map<String, String> collectLabels(RateLimitResp rateLimitResp, Metadata headers) {
        Map<String, String> finalLabels = new HashMap<>();

        List<Rule> routes = rateLimitResp.rules;

        Set<String> labelKeys = new HashSet<>();

        routes.forEach(rule -> {
            if (rule.hasDisable()) {
                return;
            }
            labelKeys.addAll(rule.getLabelsMap().keySet());
        });

        PolarisHelper.autoCollectLabels(headers, finalLabels, labelKeys);

        Map<String, String> customerLabels = PolarisHelper.getLabelsInject().injectRateLimitLabels(headers);
        finalLabels.putAll(customerLabels);
        return finalLabels;
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
