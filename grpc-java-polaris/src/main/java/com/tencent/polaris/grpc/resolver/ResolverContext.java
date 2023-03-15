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

package com.tencent.polaris.grpc.resolver;

import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.api.SDKContext;

import java.net.URI;

public class ResolverContext {

    private URI targetUri;

    private SDKContext context;

    private ServiceKey sourceService;

    public URI getTargetUri() {
        return targetUri;
    }

    public SDKContext getContext() {
        return context;
    }

    public ServiceKey getSourceService() {
        return sourceService;
    }

    public static ResolverContextBuilder builder() {
        return new ResolverContextBuilder();
    }

    public static final class ResolverContextBuilder {
        private URI targetUri;
        private SDKContext context;
        private ServiceKey sourceService;

        private ResolverContextBuilder() {
        }

        public ResolverContextBuilder targetUri(URI targetUri) {
            this.targetUri = targetUri;
            return this;
        }

        public ResolverContextBuilder context(SDKContext context) {
            this.context = context;
            return this;
        }

        public ResolverContextBuilder sourceService(ServiceKey sourceService) {
            this.sourceService = sourceService;
            return this;
        }

        public ResolverContext build() {
            ResolverContext resolverContext = new ResolverContext();
            resolverContext.context = this.context;
            resolverContext.targetUri = this.targetUri;
            resolverContext.sourceService = this.sourceService;
            return resolverContext;
        }
    }
}
