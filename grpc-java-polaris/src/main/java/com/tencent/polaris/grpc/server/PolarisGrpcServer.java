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
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.grpc.util.IpUtil;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author lixiaoshuang
 */
public class PolarisGrpcServer extends Server {

    private final Logger log = LoggerFactory.getLogger(PolarisGrpcServer.class);

    private final SDKContext context = SDKContext.initContext();

    private final ProviderAPI providerAPI = DiscoveryAPIFactory.createProviderAPIByContext(context);

    private final PolarisGrpcServerBuilder builder;

    private Server targetServer;

    private final AtomicBoolean shutdownOnce = new AtomicBoolean(false);

    private String host;

    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1, r -> {
        Thread t = new Thread(r);
        t.setName("polaris-grpc-server");
        return t;
    });

    PolarisGrpcServer(PolarisGrpcServerBuilder builder, Server server) {
        this.builder = builder;
        this.targetServer = server;
    }

    @Override
    public Server start() throws IOException {
        initLocalHost();
        targetServer = targetServer.start();
        this.registerInstance(targetServer.getServices());
        return this;
    }

    @Override
    public Server shutdown() {
        if (shutdownOnce.compareAndSet(false, true)) {
            executorService.shutdownNow();
            this.deregister(targetServer.getServices());
            providerAPI.destroy();
        }
        return this.targetServer.shutdown();
    }

    @Override
    public Server shutdownNow() {
        if (shutdownOnce.compareAndSet(false, true)) {
            executorService.shutdownNow();
            this.deregister(targetServer.getServices());
            providerAPI.destroy();
        }
        return this.targetServer.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return this.targetServer.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.targetServer.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.targetServer.awaitTermination(timeout, unit);
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        this.targetServer.awaitTermination();
    }

    private void initLocalHost() {
        host = builder.getHost();
        if (StringUtils.isNoneBlank(host)) {
            return;
        }
        String polarisServerAddr = context.getConfig().getGlobal().getServerConnector().getAddresses().get(0);
        String[] detail = StringUtils.split(polarisServerAddr, ":");
        host = IpUtil.getLocalHost(detail[0], Integer.parseInt(detail[1]));
    }

    /**
     * This interface will determine whether it is an interface-level registration instance or an application-level
     * instance registration based on grpcServiceRegister
     */
    private void registerInstance(List<ServerServiceDefinition> definitions) {
        if (StringUtils.isNoneBlank(builder.getApplicationName())) {
            this.registerOne(builder.getApplicationName());
            return;
        }
        for (ServerServiceDefinition definition : definitions) {
            String grpcServiceName = definition.getServiceDescriptor().getName();
            this.registerOne(grpcServiceName);
        }
    }

    /**
     * Register a service instance
     */
    private void registerOne(String serviceName) {
        InstanceRegisterRequest request = new InstanceRegisterRequest();
        request.setNamespace(builder.getNamespace());
        request.setService(serviceName);
        request.setHost(host);
        request.setPort(targetServer.getPort());
        request.setTtl(builder.getTtl());
        request.setMetadata(builder.getMetaData());
        InstanceRegisterResponse response = providerAPI.register(request);
        log.info("grpc server register polaris success,instanceId:{}", response.getInstanceId());
        this.heartBeat(serviceName);
    }

    /**
     * Report heartbeat
     */
    private void heartBeat(String serviceName) {
        final int ttl = builder.getTtl();
        final int port = targetServer.getPort();
        final String namespace = builder.getNamespace();
        executorService.scheduleAtFixedRate(() -> {
            log.info("Report service heartbeat");
            InstanceHeartbeatRequest request = new InstanceHeartbeatRequest();
            request.setNamespace(namespace);
            request.setService(serviceName);
            request.setHost(host);
            request.setPort(port);
            try {
                providerAPI.heartbeat(request);
            } catch (PolarisException e) {
                log.error("Report service heartbeat error!", e);
            }
        }, ttl / 2, ttl, TimeUnit.SECONDS);
    }

    /**
     * Service deregister
     */
    private void deregister(List<ServerServiceDefinition> definitions) {
        log.info("Virtual machine shut down deregister service");
        if (StringUtils.isNoneBlank(builder.getApplicationName())) {
            this.deregisterOne(builder.getApplicationName());
            return;
        }
        for (ServerServiceDefinition definition : definitions) {
            String grpcServiceName = definition.getServiceDescriptor().getName();
            this.deregisterOne(grpcServiceName);
        }
    }

    /**
     * deregister a service instance
     */
    private void deregisterOne(String serviceName) {
        InstanceDeregisterRequest request = new InstanceDeregisterRequest();
        request.setNamespace(builder.getNamespace());
        request.setService(serviceName);
        request.setHost(host);
        request.setPort(targetServer.getPort());
        providerAPI.deRegister(request);
    }
}
