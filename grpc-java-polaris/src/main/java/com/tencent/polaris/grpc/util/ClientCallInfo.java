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
