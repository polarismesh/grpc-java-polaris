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
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    
    
    public PolarisNameResolver(URI targetUri, ConsumerAPI consumerAPI) {
        this.service = targetUri.getHost();
        this.namespace = targetUri.getQuery().split("=")[1];
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
        log.debug("getInstances response:{}", response);
        List<EquivalentAddressGroup> equivalentAddressGroups = null;
        if (Objects.nonNull(response)) {
            equivalentAddressGroups = Arrays.stream(response.getInstances()).filter(Instance::isHealthy)
                    .map(instance -> new EquivalentAddressGroup(
                            new InetSocketAddress(instance.getHost(), instance.getPort())))
                    .collect(Collectors.toList());
        }
        listener.onAddresses(equivalentAddressGroups, Attributes.EMPTY);
    }
    
    @Override
    public void shutdown() {
    
    }
    
}
