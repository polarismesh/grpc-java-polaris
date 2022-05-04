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

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.grpc.interceptor.PolarisServerInterceptor;
import com.tencent.polaris.grpc.util.GrpcHelper;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC-Server 端限流拦截器
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisRateLimitServerInterceptor extends PolarisServerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(PolarisRateLimitServerInterceptor.class);

    private static final String GRPC_SERVICE_LABEL_KEY = "__grpc_service__";

    private LimitAPI limitAPI;

    private String namespace = "default";

    private String applicationName = "";

    private String customKeyPrefix = "";

    private Set<String> customLabels = Collections.emptySet();

    private BiFunction<QuotaResponse, String, Status> rateLimitCallback;

    private Predicate<String> predicate = (key) -> {
        if (key.startsWith(customKeyPrefix)) {
            return true;
        }
        if (customLabels.contains(key)) {
            return true;
        }

        return false;
    };

    public PolarisRateLimitServerInterceptor() {
    }

    public void setCustomKeyPrefix(String customKeyPrefix) {
        this.customKeyPrefix = customKeyPrefix;
    }

    public void setCustomLabels(Set<String> customLabels) {
        this.customLabels = customLabels;
    }

    public void setRateLimitCallback(BiFunction<QuotaResponse, String, Status> rateLimitCallback) {
        this.rateLimitCallback = rateLimitCallback;
    }

    @Override
    public void init(final String namespace, final String applicationName, SDKContext context) {
        this.namespace = namespace;
        this.applicationName = applicationName;
        this.limitAPI = LimitAPIFactory.createLimitAPIByContext(context);
    }

    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        final String applicationName = this.applicationName;
        final String serviceName = call.getMethodDescriptor().getServiceName();
        final String method = call.getMethodDescriptor().getBareMethodName();
        final Map<String, String> labels = collectLabels(headers);

        final QuotaRequest request = new QuotaRequest();
        request.setNamespace(namespace);
        if (StringUtils.isNotBlank(applicationName)) {
            request.setService(applicationName);
            labels.put(GRPC_SERVICE_LABEL_KEY, serviceName);
        } else {
            request.setService(serviceName);
        }
        request.setMethod(method);
        request.setCount(1);
        request.setLabels(labels);

        LOG.debug("do get quota, request : {}", request);

        final QuotaResponse response = limitAPI.getQuota(request);
        if (Objects.equals(response.getCode(), QuotaResultCode.QuotaResultOk)) {
            return next.startCall(call, headers);
        }

        Status errStatus = rateLimitCallback.apply(response, call.getMethodDescriptor().getFullMethodName());
        call.close(errStatus, headers);
        return new ServerCall.Listener<ReqT>() {};
    }

    private Map<String, String> collectLabels(Metadata headers) {
        Map<String, String> labels = new HashMap<>();

        boolean hasPrefix = StringUtils.isNotBlank(customKeyPrefix);
        boolean hasCustomLabels = customLabels.isEmpty();

        if (!hasPrefix || !hasCustomLabels) {
            return labels;
        }

        labels = GrpcHelper.collectLabels(headers, predicate);

        return labels;
    }


}
