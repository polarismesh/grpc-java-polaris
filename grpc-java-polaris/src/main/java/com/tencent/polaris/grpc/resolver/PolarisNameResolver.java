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

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.listener.ServiceListener;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceChangeEvent;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.GetHealthyInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.UnWatchServiceRequest;
import com.tencent.polaris.api.rpc.WatchServiceRequest;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.grpc.util.Common;
import com.tencent.polaris.grpc.util.NetworkHelper;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shade.polaris.com.google.gson.Gson;

/**
 * Service discovery class
 *
 * @author lixiaoshuang
 */
public class PolarisNameResolver extends NameResolver {

    private static final Logger LOG = LoggerFactory.getLogger(PolarisNameResolver.class);

    private static final String DEFAULT_NAMESPACE = "default";

    private final ConsumerAPI consumerAPI;

    private final String namespace;

    private final String service;

    private ServiceChangeWatcher watcher;

    private final SDKContext context;

    private Listener2 listener;

    private ServiceInfo sourceService;

    public PolarisNameResolver(URI targetUri, SDKContext context, ConsumerAPI consumerAPI) {
        Map<String, String> params = NetworkHelper.getUrlParams(targetUri.getQuery());

        this.service = targetUri.getHost();
        this.namespace = params.get("namespace") == null ? DEFAULT_NAMESPACE : params.get("namespace");
        this.context = context;
        this.consumerAPI = consumerAPI;

        if (params.containsKey("extend_info")) {
            this.sourceService = new Gson().fromJson(new String(Base64.getUrlDecoder().decode(params.get("extend_info"))),
                    ServiceInfo.class);
        }
    }

    @Override
    public String getServiceAuthority() {
        return service;
    }

    @Override
    public void start(Listener2 listener) {
        GetHealthyInstancesRequest request = new GetHealthyInstancesRequest();
        request.setNamespace(namespace);
        request.setService(service);
        InstancesResponse response = consumerAPI.getHealthyInstancesInstance(request);
        LOG.info("[grpc-polaris] namespace:{} service:{} instance size:{}", namespace, service,
                response.getInstances().length);
        this.listener = listener;
        notifyListener(listener, response);
        doWatch(listener);
    }

    private void doWatch(Listener2 listener) {
        this.watcher = new ServiceChangeWatcher(listener);
        this.consumerAPI.watchService(WatchServiceRequest.builder()
                .namespace(namespace)
                .service(service)
                .listeners(Collections.singletonList(this.watcher))
                .build());
    }

    private void notifyListener(Listener2 listener, InstancesResponse response) {
        ServiceInstances serviceInstances = response.toServiceInstances();
        if (!serviceInstances.getInstances().isEmpty()) {
            List<EquivalentAddressGroup> equivalentAddressGroups = serviceInstances.getInstances()
                    .stream()
                    .map(this::buildEquivalentAddressGroup)
                    .collect(Collectors.toList());

            Attributes.Builder builder = Attributes.newBuilder();

            if (sourceService != null) {
                builder.set(Common.SOURCE_SERVICE_INFO, sourceService);
            }

            listener.onResult(ResolutionResult.newBuilder()
                            .setAddresses(equivalentAddressGroups)
                            .setAttributes(builder.build())
                    .build());
        }
    }

    @Override
    public void shutdown() {
        if (this.watcher != null) {
            this.consumerAPI.unWatchService(UnWatchServiceRequest.UnWatchServiceRequestBuilder.anUnWatchServiceRequest()
                    .listeners(Collections.singletonList(this.watcher))
                    .namespace(namespace)
                    .service(service)
                    .build());
        }
    }

    private class ServiceChangeWatcher implements ServiceListener {

        private final Listener2 listener;

        ServiceChangeWatcher(Listener2 listener) {
            this.listener = listener;
        }

        @Override
        public void onEvent(ServiceChangeEvent event) {
            LOG.info("[grpc-polaris] receive:{} service:{} final instance size:{}", namespace, service,
                    event.getAllInstances().size());

            GetHealthyInstancesRequest request = new GetHealthyInstancesRequest();
            request.setNamespace(namespace);
            request.setService(service);
            InstancesResponse response = consumerAPI.getHealthyInstancesInstance(request);
            notifyListener(listener, response);
        }

    }

    private EquivalentAddressGroup buildEquivalentAddressGroup(Instance instance) {
        InetSocketAddress address = new InetSocketAddress(instance.getHost(), instance.getPort());
        Attributes attributes = Attributes.newBuilder()
                .set(Common.INSTANCE_KEY, instance)
                .set(Common.TARGET_NAMESPACE_KEY, namespace)
                .set(Common.TARGET_SERVICE_KEY, service)
                .build();
        return new EquivalentAddressGroup(address, attributes);
    }

}
