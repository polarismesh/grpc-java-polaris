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

package com.tencent.polaris.grpc.server;

import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.grpc.util.IpUtil;
import com.tencent.polaris.grpc.util.JvmShutdownHookUtil;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author lixiaoshuang
 */
public class PolarisGrpcServer {
    
    private final Logger log = LoggerFactory.getLogger(PolarisGrpcServer.class);
    
    private final ProviderAPI providerAPI = DiscoveryAPIFactory.createProviderAPI();
    
    private final int ttl = 5;
    
    private final int port;
    
    private final String serviceName;
    
    private final String namespace;
    
    private final Map<String, String> metaData;
    
    private final List<BindableService> bindableServices;
    
    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(5);
    
    private PolarisGrpcServer(Builder builder) {
        this.port = builder.port;
        this.serviceName = builder.serviceName;
        this.namespace = builder.namespace;
        this.bindableServices = builder.bindableServices;
        this.metaData = builder.metaData;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public boolean start() {
        if (port <= 0) {
            log.error("abnormal port");
            return false;
        }
        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);
        if (CollectionUtils.isNotEmpty(bindableServices)) {
            for (BindableService bindableService : bindableServices) {
                serverBuilder.addService(bindableService);
            }
        }
        Server server;
        try {
            server = serverBuilder.build();
            server = server.start();
            log.info("grpc server started at port : {}", port);
            
            if (!server.isShutdown() && !server.isTerminated()) {
                this.registerInstance();
            }
            
            Server finalServer = server;
            JvmShutdownHookUtil.addHook(() -> {
                log.info("shutting sown grpc server sine JVM is shutting down");
                executorService.shutdownNow();
                deregister();
                providerAPI.destroy();
                finalServer.shutdown();
            });
            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            log.error("grpc server started error, msg: {}", e.getMessage());
            return false;
        }
        return true;
    }
    
    
    public static class Builder {
        
        private int port;
        
        private String serviceName;
        
        private String namespace;
        
        private List<BindableService> bindableServices;
        
        private Map<String, String> metaData;
        
        
        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }
        
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }
        
        public Builder bindableServices(List<BindableService> bindableServices) {
            this.bindableServices = bindableServices;
            return this;
        }
        
        public Builder metaData(Map<String, String> metadata) {
            this.metaData = metadata;
            return this;
        }
        
        public PolarisGrpcServer build() {
            return new PolarisGrpcServer(this);
        }
    }
    
    
    /**
     * Register service
     */
    private void registerInstance() {
        InstanceRegisterRequest request = new InstanceRegisterRequest();
        request.setNamespace(namespace);
        request.setService(serviceName);
        request.setHost(IpUtil.getLocalHost());
        request.setPort(port);
        request.setTtl(ttl);
        request.setMetadata(metaData);
        InstanceRegisterResponse response = providerAPI.register(request);
        log.info("grpc server register polaris success,instanceId:{}", response.getInstanceId());
        this.heartBeat();
    }
    
    
    /**
     * Report heartbeat
     */
    private void heartBeat() {
        executorService.scheduleAtFixedRate(() -> {
            log.info("Report service heartbeat");
            InstanceHeartbeatRequest request = new InstanceHeartbeatRequest();
            request.setNamespace(namespace);
            request.setService(serviceName);
            request.setHost(IpUtil.getLocalHost());
            request.setPort(port);
            try {
                providerAPI.heartbeat(request);
            } catch (PolarisException e) {
                log.error("Report service heartbeat error!", e);
            }
        }, 1, ttl, TimeUnit.SECONDS);
    }
    
    /**
     * Service deregister
     */
    private void deregister() {
        log.info("Virtual machine shut down deregister service");
        InstanceDeregisterRequest request = new InstanceDeregisterRequest();
        request.setNamespace(namespace);
        request.setService(serviceName);
        request.setHost(IpUtil.getLocalHost());
        request.setPort(port);
        providerAPI.deRegister(request);
    }
}
