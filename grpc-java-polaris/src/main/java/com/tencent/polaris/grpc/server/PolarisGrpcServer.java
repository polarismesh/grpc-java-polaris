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
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.grpc.util.IpUtil;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
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
    
    private List<BindableService> bindableServices;
    
    private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(5);
    
    public PolarisGrpcServer(int port, String namespace, String serviceName, List<BindableService> bindableServices) {
        this.port = port;
        this.namespace = namespace;
        this.serviceName = serviceName;
        this.bindableServices = bindableServices;
    }
    
    public void start() {
        if (port <= 0) {
            log.error("abnormal port");
            return;
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
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("shutting sown grpc server sine JVM is shutting down");
                executorService.shutdownNow();
                deregister();
                providerAPI.destroy();
                finalServer.shutdown();
            }));
            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            log.error("grpc server started error, msg: {}", e.getMessage());
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
            providerAPI.heartbeat(request);
        }, 0, ttl, TimeUnit.SECONDS);
    }
    
    /**
     * Service un registration
     */
    private void deregister() {
        log.info("Virtual machine shut down Anti-registration service");
        InstanceDeregisterRequest request = new InstanceDeregisterRequest();
        request.setNamespace(namespace);
        request.setService(serviceName);
        request.setHost(IpUtil.getLocalHost());
        request.setPort(port);
        providerAPI.deRegister(request);
        providerAPI.destroy();
    }
}
