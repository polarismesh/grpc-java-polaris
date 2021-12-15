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
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.grpc.util.IpUtil;
import com.tencent.polaris.grpc.util.JvmShutdownHookUtil;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
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
    
    private final int ttl;
    
    private final int port;
    
    private final String applicationName;
    
    private final String namespace;
    
    private final Map<String, String> metaData;
    
    private final List<BindableService> bindableServices;
    
    private final String siteLocalIp;
    
    private final boolean grpcServiceRegister;
    
    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1, r -> {
        Thread t = new Thread(r);
        t.setName("polaris-grpc-server");
        return t;
    });
    
    
    private PolarisGrpcServer(Builder builder) {
        this.ttl = builder.ttl;
        this.port = builder.port;
        this.applicationName = builder.applicationName;
        this.namespace = builder.namespace;
        this.metaData = builder.metaData;
        this.bindableServices = builder.bindableServices;
        this.siteLocalIp = builder.siteLocalIp;
        this.grpcServiceRegister = builder.grpcServiceRegister;
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
                this.registerInstance(bindableServices);
            }
            
            Server finalServer = server;
            JvmShutdownHookUtil.addHook(() -> {
                log.info("shutting sown grpc server sine JVM is shutting down");
                executorService.shutdownNow();
                this.deregister(bindableServices);
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
        
        private String applicationName;
        
        private String namespace;
        
        private List<BindableService> bindableServices;
        
        private Map<String, String> metaData;
        
        private int ttl;
        
        private String siteLocalIp;
        
        private boolean grpcServiceRegister;
        
        private static final String DEFAULT_NAMESPACE = "default";
        
        private static final int DEFAULT_TTL = 5;
        
        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder applicationName(String applicationName) {
            this.applicationName = applicationName;
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
        
        public Builder ttl(int ttl) {
            this.ttl = ttl;
            return this;
        }
        
        public Builder siteLocalIp(String siteLocalIp) {
            this.siteLocalIp = siteLocalIp;
            return this;
        }
        
        public PolarisGrpcServer build() {
            checkField();
            return new PolarisGrpcServer(this);
        }
        
        private void checkField() {
            if (StringUtils.isBlank(applicationName)) {
                this.grpcServiceRegister = true;
            }
            if (StringUtils.isBlank(namespace)) {
                this.namespace = DEFAULT_NAMESPACE;
            }
            if (ttl == 0) {
                this.ttl = DEFAULT_TTL;
            }
            if (StringUtils.isBlank(siteLocalIp)) {
                this.siteLocalIp = IpUtil.getLocalHostExactAddress();
            }
        }
    }
    
    
    /**
     * This interface will determine whether it is an interface-level registration instance or an application-level
     * instance registration based on grpcServiceRegister
     */
    private void registerInstance(List<BindableService> bindableServices) {
        if (grpcServiceRegister) {
            for (BindableService bindableService : bindableServices) {
                ServerServiceDefinition serverServiceDefinition = bindableService.bindService();
                String grpcServiceName = serverServiceDefinition.getServiceDescriptor().getName();
                this.registerOne(grpcServiceName);
            }
            return;
        }
        this.registerOne(applicationName);
    }
    
    /**
     * Register a service instance
     *
     */
    private void registerOne(String useServiceName) {
        InstanceRegisterRequest request = new InstanceRegisterRequest();
        request.setNamespace(namespace);
        request.setService(useServiceName);
        request.setHost(siteLocalIp);
        request.setPort(port);
        request.setTtl(ttl);
        request.setMetadata(metaData);
        InstanceRegisterResponse response = providerAPI.register(request);
        log.info("grpc server register polaris success,instanceId:{}", response.getInstanceId());
        this.heartBeat(useServiceName);
    }
    
    /**
     * Report heartbeat
     */
    private void heartBeat(String useServiceName) {
        executorService.scheduleAtFixedRate(() -> {
            log.info("Report service heartbeat");
            InstanceHeartbeatRequest request = new InstanceHeartbeatRequest();
            request.setNamespace(namespace);
            request.setService(useServiceName);
            request.setHost(siteLocalIp);
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
    private void deregister(List<BindableService> bindableServices) {
        log.info("Virtual machine shut down deregister service");
        if (grpcServiceRegister) {
            for (BindableService bindableService : bindableServices) {
                ServerServiceDefinition serverServiceDefinition = bindableService.bindService();
                String grpcServiceName = serverServiceDefinition.getServiceDescriptor().getName();
                this.deregisterOne(grpcServiceName);
            }
            return;
        }
        this.deregisterOne(applicationName);
    }
    
    /**
     * deregister a service instance
     *
     */
    private void deregisterOne(String useServiceName) {
        InstanceDeregisterRequest request = new InstanceDeregisterRequest();
        request.setNamespace(namespace);
        request.setService(useServiceName);
        request.setHost(siteLocalIp);
        request.setPort(port);
        providerAPI.deRegister(request);
    }
}
