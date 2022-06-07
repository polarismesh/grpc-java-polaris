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

package com.tencent.polaris.grpc.util;

import com.tencent.polaris.grpc.client.MetadataClientInterceptor;
import com.tencent.polaris.grpc.ratelimit.PolarisRateLimitServerInterceptor;
import com.tencent.polaris.grpc.server.MetadataServerInterceptor;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import io.grpc.ClientInterceptor;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisHelper {

    /**
     * {@link PolarisLabelsInject} 用户自定义的 PolarisLabelsInject 实现，可以在处理每次流量时，通过
     * {@link PolarisLabelsInject#injectRoutingLabels(Metadata)} 或者 {@link PolarisLabelsInject#injectRoutingLabels(Metadata)}
     * 注入本次流量的标签信息
     */
    private static PolarisLabelsInject LABELS_INJECT;

    static {
        ServiceLoader<PolarisLabelsInject> serviceLoader = ServiceLoader.load(PolarisLabelsInject.class);
        Iterator<PolarisLabelsInject> iterator = serviceLoader.iterator();
        LABELS_INJECT = Optional.ofNullable(iterator.hasNext() ? iterator.next() : null).orElse(new PolarisLabelsInject() {
            @Override
            public Map<String, String> injectRoutingLabels(Metadata metadata) {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, String> injectRateLimitLabels(Metadata metadata) {
                return Collections.emptyMap();
            }
        });
    }

    /**
     * 调用此方法注入用户自定义的 PolarisLabelsInject
     *
     * @param inject {@link PolarisLabelsInject}
     */
    public static void setLabelsInject(PolarisLabelsInject inject) {
        LABELS_INJECT = inject;
    }

    public static PolarisLabelsInject getLabelsInject() {
        return LABELS_INJECT;
    }

    /**
     * 根据 gRPC 的 header、Context 以及对应的 labelKeys 规则信息，自动注入相关的 labels
     *
     * @param headers {@link Metadata}
     * @param finalLabels {@link Map<String, String>}
     * @param labelKeys {@link Set<String>}
     */
    public static void autoCollectLabels(Metadata headers, Map<String, String> finalLabels, Set<String> labelKeys) {
        for (String label : labelKeys) {
            if (label.startsWith(Common.GRPC_HEADER_PREFIX)) {
                String newLabel = label.substring(Common.GRPC_HEADER_PREFIX_LEN);
                finalLabels.put(label, headers.get(Key.of(newLabel, Metadata.ASCII_STRING_MARSHALLER)));
                continue;
            }

            if (label.startsWith(Common.GRPC_CONTEXT_PREFIX)) {
                String newLabel = label.substring(Common.GRPC_CONTEXT_PREFIX_LEN);
                finalLabels.put(label, String.valueOf(Context.key(newLabel).get()));
            }

            if (label.startsWith(Common.GRPC_SYSTEM_ENV_PREFIX)) {
                String newLabel = label.substring(Common.GRPC_CONTEXT_PREFIX_LEN);
                finalLabels.put(label, System.getenv(newLabel));
            }
        }
    }

    public static ClientInterceptor buildMetadataClientInterceptor() {
        return new MetadataClientInterceptor(s -> true);
    }


    public static ClientInterceptor buildMetadataClientInterceptor(Predicate<String> predicate) {
        return new MetadataClientInterceptor(predicate);
    }

    public static ServerInterceptor buildMetadataServerInterceptor() {
        return new MetadataServerInterceptor();
    }

    /**
     * 使用 builder 模式开启 gRPC 的限流能力
     *
     * @return {@link PolarisRateLimitInterceptorBuilder}
     */
    public static PolarisRateLimitInterceptorBuilder buildRateLimitInterceptor() {
        return new PolarisRateLimitInterceptorBuilder();
    }

    public static class PolarisRateLimitInterceptorBuilder {

        private BiFunction<QuotaResponse, String, Status> rateLimitCallback = (quotaResponse, method) ->
                Status.UNAVAILABLE.withDescription("rate-limit exceeded (server side)");

        private PolarisRateLimitInterceptorBuilder() {
        }

        /**
         * 当限流触发时，用户自定义的限流结果返回器
         *
         * @param rateLimitCallback {@link BiFunction<QuotaResponse, String, Status>}
         * @return {@link PolarisRateLimitInterceptorBuilder}
         */
        public PolarisRateLimitInterceptorBuilder rateLimitCallback(
                BiFunction<QuotaResponse, String, Status> rateLimitCallback) {
            this.rateLimitCallback = rateLimitCallback;
            return this;
        }

        public PolarisRateLimitServerInterceptor build() {
            PolarisRateLimitServerInterceptor polarisRateLimitInterceptor = new PolarisRateLimitServerInterceptor();
            polarisRateLimitInterceptor.setRateLimitCallback(this.rateLimitCallback);
            return polarisRateLimitInterceptor;
        }

    }

}
