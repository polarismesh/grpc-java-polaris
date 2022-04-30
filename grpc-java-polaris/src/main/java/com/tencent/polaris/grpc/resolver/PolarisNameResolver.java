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
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.UnWatchServiceRequest;
import com.tencent.polaris.api.rpc.WatchServiceRequest;
import com.tencent.polaris.grpc.util.Common;
import io.grpc.Attributes;
import io.grpc.Attributes.Key;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service discovery class
 *
 * @author lixiaoshuang
 */
public class PolarisNameResolver extends NameResolver {

    private final Logger log = LoggerFactory.getLogger(PolarisNameResolver.class);

    private final ConsumerAPI consumerAPI;

    private final String namespace;

    private final String service;

    private static final String DEFAULT_NAMESPACE = "default";

    private ServiceChangeWatcher watcher;

    public PolarisNameResolver(URI targetUri, ConsumerAPI consumerAPI) {
        this.service = targetUri.getHost();
        this.namespace = targetUri.getQuery() == null ? DEFAULT_NAMESPACE : targetUri.getQuery().split("=")[1];
        this.consumerAPI = consumerAPI;
    }

    @Override
    public String getServiceAuthority() {
        return service;
    }

    @Override
    public void start(Listener listener) {
        GetInstancesRequest request = new GetInstancesRequest();
        request.setNamespace(namespace);
        request.setService(service);
        InstancesResponse response = consumerAPI.getInstances(request);
        log.info("namespace:{} service:{} instance size:{}", namespace, service,
                response.getInstances().length);
        notifyListener(listener, response);
        doWatch(listener);
    }

    private void doWatch(Listener listener) {
        this.watcher = new ServiceChangeWatcher(listener);
        consumerAPI.watchService(WatchServiceRequest.builder()
                .namespace(namespace)
                .service(service)
                .listeners(Collections.singletonList(this.watcher))
                .build());
    }

    private void notifyListener(Listener listener, InstancesResponse response) {
        List<EquivalentAddressGroup> equivalentAddressGroups = null;
        if (Objects.nonNull(response)) {
            equivalentAddressGroups = Arrays.stream(response.getInstances()).filter(Instance::isHealthy)
                    .map(PolarisNameResolver::buildEquivalentAddressGroup)
                    .collect(Collectors.toList());
        }
        listener.onAddresses(equivalentAddressGroups, Attributes.EMPTY);
    }

    @Override
    public void shutdown() {
        if (this.watcher != null) {
            consumerAPI.unWatchService(UnWatchServiceRequest.UnWatchServiceRequestBuilder.anUnWatchServiceRequest()
                    .listeners(Collections.singletonList(this.watcher))
                    .namespace(namespace)
                    .service(service)
                    .build());
        }
    }

    private class ServiceChangeWatcher implements ServiceListener {

        private final Listener listener;

        ServiceChangeWatcher(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void onEvent(ServiceChangeEvent event) {
            log.info("receive:{} service:{} final instance size:{}", namespace, service,
                    event.getAllInstances().size());
            List<EquivalentAddressGroup> equivalentAddressGroups = event.getAllInstances()
                    .stream()
                    .filter(Instance::isHealthy)
                    .map(PolarisNameResolver::buildEquivalentAddressGroup)
                    .collect(Collectors.toList());
            listener.onAddresses(equivalentAddressGroups, Attributes.EMPTY);
        }

    }

    private static EquivalentAddressGroup buildEquivalentAddressGroup(Instance instance) {
        InetSocketAddress address = new InetSocketAddress(instance.getHost(), instance.getPort());
        Attributes attributes = Attributes.newBuilder().set(Common.INSTANCE_KEY, instance).build();
        return new EquivalentAddressGroup(address, attributes);
    }

}
