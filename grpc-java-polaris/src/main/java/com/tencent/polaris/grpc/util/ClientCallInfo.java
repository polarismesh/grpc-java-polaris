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

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.Instance;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class ClientCallInfo {

    private final String method;
    private final Instance instance;
    private final ConsumerAPI consumerAPI;
    private final String targetNamespace;
    private final String targetService;

    public ClientCallInfo(String method, Instance instance, ConsumerAPI consumerAPI,
            String targetNamespace, String targetService) {
        this.method = method;
        this.instance = instance;
        this.consumerAPI = consumerAPI;
        this.targetNamespace = targetNamespace;
        this.targetService = targetService;
    }

    public String getMethod() {
        return method;
    }

    public Instance getInstance() {
        return instance;
    }

    public ConsumerAPI getConsumerAPI() {
        return consumerAPI;
    }

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public String getTargetService() {
        return targetService;
    }

    public static ClientCallInfoBuilder builder() {
        return new ClientCallInfoBuilder();
    }

    public static final class ClientCallInfoBuilder {
        private String method;
        private Instance instance;
        private ConsumerAPI consumerAPI;
        private String targetNamespace;
        private String targetService;

        private ClientCallInfoBuilder() {
        }

        public ClientCallInfoBuilder method(String method) {
            this.method = method;
            return this;
        }

        public ClientCallInfoBuilder instance(Instance instance) {
            this.instance = instance;
            return this;
        }

        public ClientCallInfoBuilder consumerAPI(ConsumerAPI consumerAPI) {
            this.consumerAPI = consumerAPI;
            return this;
        }

        public ClientCallInfoBuilder targetNamespace(String targetNamespace) {
            this.targetNamespace = targetNamespace;
            return this;
        }

        public ClientCallInfoBuilder targetService(String targetService) {
            this.targetService = targetService;
            return this;
        }

        public ClientCallInfo build() {
            return new ClientCallInfo(method, instance, consumerAPI, targetNamespace, targetService);
        }
    }
}
