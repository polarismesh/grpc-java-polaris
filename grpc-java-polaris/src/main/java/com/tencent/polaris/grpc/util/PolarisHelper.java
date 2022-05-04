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

import com.tencent.polaris.grpc.ratelimit.PolarisRateLimitServerInterceptor;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import io.grpc.Status;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisHelper {

    public static PolarisRateLimitInterceptorBuilder buildRateLimitInterceptor() {
        return new PolarisRateLimitInterceptorBuilder();
    }

    public static class PolarisRateLimitInterceptorBuilder {

        private String customKeyPrefix = "";

        private Set<String> customLabels = Collections.emptySet();

        private BiFunction<QuotaResponse, String, Status> rateLimitCallback = (quotaResponse, method) ->
                Status.UNAVAILABLE.withDescription("rate-limit exceeded (server side)");

        private PolarisRateLimitInterceptorBuilder() {
        }

        public PolarisRateLimitInterceptorBuilder customKeyPrefix(String customKeyPrefix) {
            this.customKeyPrefix = customKeyPrefix;
            return this;
        }

        public PolarisRateLimitInterceptorBuilder customLabels(Set<String> customLabels) {
            this.customLabels = customLabels;
            return this;
        }

        public PolarisRateLimitInterceptorBuilder rateLimitCallback(
                BiFunction<QuotaResponse, String, Status> rateLimitCallback) {
            this.rateLimitCallback = rateLimitCallback;
            return this;
        }

        public PolarisRateLimitServerInterceptor build() {
            PolarisRateLimitServerInterceptor polarisRateLimitInterceptor = new PolarisRateLimitServerInterceptor();
            polarisRateLimitInterceptor.setCustomKeyPrefix(this.customKeyPrefix);
            polarisRateLimitInterceptor.setCustomLabels(this.customLabels);
            polarisRateLimitInterceptor.setRateLimitCallback(this.rateLimitCallback);
            return polarisRateLimitInterceptor;
        }

    }

}
