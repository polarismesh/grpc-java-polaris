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
public class ServerAgent {
    
    private final Logger log = LoggerFactory.getLogger(ServerAgent.class);
    
    private final ProviderAPI providerAPI = DiscoveryAPIFactory.createProviderAPI();
    
    private int port;
    
    private String service;
    
    private List<BindableService> bindableServices;
    
    private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(5);
    
    public ServerAgent(int port, String service, List<BindableService> bindableServices) {
        this.port = port;
        this.service = service;
        this.bindableServices = bindableServices;
    }
    
    public void start() {
        if (port <= 0) {
            log.error("abnormal port");
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
                finalServer.shutdown();
                deRegister();
            }));
            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            log.error("grpc server started error, msg: {}", e.getMessage());
        }
    }
    
    /**
     * 注册服务
     */
    private void registerInstance() {
        InstanceRegisterRequest request = new InstanceRegisterRequest();
        request.setNamespace("default");
        request.setService(service);
        request.setHost(IpUtil.getLocalHost());
        request.setPort(port);
        request.setTtl(2);
        InstanceRegisterResponse response = providerAPI.register(request);
        log.info("grpc server register polaris success,instanceId:{}", response.getInstanceId());
        this.heartBeat();
    }
    
    
    /**
     * 上报心跳
     */
    private void heartBeat() {
        executorService.scheduleAtFixedRate(() -> {
            log.info("Report service heartbeat");
            InstanceHeartbeatRequest request = new InstanceHeartbeatRequest();
            request.setNamespace("default");
            request.setService(service);
            request.setHost(IpUtil.getLocalHost());
            request.setPort(port);
            providerAPI.heartbeat(request);
        }, 0, 500, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 服务反注册
     */
    private void deRegister() {
        log.info("Virtual machine shut down Anti-registration service");
        InstanceDeregisterRequest request = new InstanceDeregisterRequest();
        request.setNamespace("default");
        request.setService(service);
        request.setHost(IpUtil.getLocalHost());
        request.setPort(port);
        providerAPI.deRegister(request);
        providerAPI.destroy();
    }
}
