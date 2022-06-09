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

import com.google.common.util.concurrent.MoreExecutors;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.grpc.interceptor.PolarisServerInterceptor;
import io.grpc.BindableService;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.HandlerRegistry;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public final class PolarisGrpcServerBuilder extends ServerBuilder<PolarisGrpcServerBuilder> {

    private static final String DEFAULT_NAMESPACE = "default";

    private static final int DEFAULT_TTL = 5;

    private String applicationName;

    private String namespace;

    private Map<String, String> metaData = new HashMap<>();

    private int weight;

    private String version;

    private int heartbeatInterval;

    private String host;

    private DelayRegister delayRegister;

    private RegisterHook registerHook;

    /**
     * gRPC-Server 优雅关闭最大等待时长
     */
    private Duration maxWaitDuration = Duration.ofSeconds(30);

    private boolean openGraceOffline = true;

    private final ServerBuilder<?> builder;

    private final List<PolarisServerInterceptor> polarisInterceptors = new ArrayList<>();

    private final List<ServerInterceptor> interceptors = new ArrayList<>();

    private final SDKContext context = SDKContext.initContext();

    /**
     * Static factory for creating a new PolarisGrpcServerBuilder.
     *
     * @param port the port to listen on
     * @return PolarisGrpcServerBuilder
     */
    public static PolarisGrpcServerBuilder forPort(int port) {
        ServerBuilder<?> builder = ServerBuilder.forPort(port);
        return new PolarisGrpcServerBuilder(builder);
    }
    
    /**
     * PolarisGrpcServerBuilder Constructor.
     *
     * @param builder ServerBuilder
     */
    public PolarisGrpcServerBuilder(ServerBuilder<?> builder) {
        this.builder = builder;
    }
    
    /**
     * Set grpc service name.
     *
     * @param applicationName grpc server name
     * @return PolarisGrpcServerBuilder
     */
    public PolarisGrpcServerBuilder applicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }
    
    /**
     * Namespace registered by grpc service.
     *
     * @param namespace polaris namespace
     * @return PolarisGrpcServerBuilder
     */
    public PolarisGrpcServerBuilder namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }
    
    /**
     * Set metadata.
     *
     * @param metadata metadata
     * @return PolarisGrpcServerBuilder
     */
    public PolarisGrpcServerBuilder metadata(Map<String, String> metadata) {
        this.metaData = metadata;
        return this;
    }

    /**
     * set instance weight
     *
     * @param weight
     * @return
     */
    public PolarisGrpcServerBuilder weight(int weight) {
        this.weight = weight;
        return this;
    }

    public PolarisGrpcServerBuilder version(String version) {
        this.version = version;
        return this;
    }

    /**
     * Set the heartbeat report time by default 5 seconds.
     *
     * @param heartbeatInterval Time in seconds
     * @return PolarisGrpcServerBuilder
     */
    public PolarisGrpcServerBuilder heartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
        return this;
    }
    
    /**
     * Set the local host.
     *
     * @param host host
     * @return PolarisGrpcServerBuilder
     */
    public PolarisGrpcServerBuilder host(String host) {
        this.host = host;
        return this;
    }

    @Override
    public PolarisGrpcServerBuilder directExecutor() {
        return executor(MoreExecutors.directExecutor());
    }

    @Override
    public PolarisGrpcServerBuilder executor(@Nullable Executor executor) {
        this.builder.executor(executor);
        return this;
    }

    @Override
    public PolarisGrpcServerBuilder addService(ServerServiceDefinition service) {
        this.builder.addService(service);
        return this;
    }

    @Override
    public PolarisGrpcServerBuilder addService(BindableService bindableService) {
        this.builder.addService(bindableService);
        return this;
    }

    @Override
    public PolarisGrpcServerBuilder fallbackHandlerRegistry(@Nullable HandlerRegistry fallbackRegistry) {
        this.builder.fallbackHandlerRegistry(fallbackRegistry);
        return this;
    }

    @Override
    public PolarisGrpcServerBuilder useTransportSecurity(File certChain, File privateKey) {
        this.builder.useTransportSecurity(certChain, privateKey);
        return this;
    }

    @Override
    public PolarisGrpcServerBuilder decompressorRegistry(@Nullable DecompressorRegistry registry) {
        this.builder.decompressorRegistry(registry);
        return this;
    }

    @Override
    public PolarisGrpcServerBuilder compressorRegistry(@Nullable CompressorRegistry registry) {
        this.builder.compressorRegistry(registry);
        return this;
    }

    @Override
    public PolarisGrpcServerBuilder intercept(ServerInterceptor interceptor) {
        if (interceptor instanceof PolarisServerInterceptor) {
            this.polarisInterceptors.add((PolarisServerInterceptor) interceptor);
        } else {
            this.interceptors.add(interceptor);
        }
        return this;
    }

    /**
     * 延迟注册, 用户可以通过设置 {@link DelayRegister} 来延迟 gRPC-server 注册到 polaris 对外提供服务的时间
     * 默认支持策略
     * - {@link com.tencent.polaris.grpc.server.impl.WaitDelayRegister} 等待一段时间在进行注册
     *
     * @param delayRegister {@link DelayRegister}
     * @return {@link PolarisGrpcServerBuilder}
     */
    public PolarisGrpcServerBuilder delayRegister(DelayRegister delayRegister) {
        this.delayRegister = delayRegister;
        return this;
    }

    /**
     * 优雅下线的最大等待时间，如果到了一定时间还没有结束，则直接强制关闭，默认 Duration.ofSeconds(30)
     *
     * @param maxWaitDuration {@link Duration}
     * @return {@link PolarisGrpcServerBuilder}
     */
    public PolarisGrpcServerBuilder maxWaitDuration(Duration maxWaitDuration) {
        this.maxWaitDuration = maxWaitDuration;
        return this;
    }

    public PolarisGrpcServerBuilder openGraceOffline(boolean openGraceOffline) {
        this.openGraceOffline = openGraceOffline;
        return this;
    }

    public PolarisGrpcServerBuilder registerHook(RegisterHook registerHook) {
        this.registerHook = registerHook;
        return this;
    }

    RegisterHook getRegisterHook() {
        return registerHook;
    }

    @Override
    public Server build() {
        setDefault();

        if (openGraceOffline) {
            // 注册统计 server 当前正在处理的请求数量
            this.builder.intercept(GraceOffline.createInterceptor());
        }

        for (PolarisServerInterceptor interceptor : polarisInterceptors) {
            interceptor.init(namespace, applicationName, context);
            this.builder.intercept(interceptor);
        }
        for (ServerInterceptor interceptor : interceptors) {
            this.builder.intercept(interceptor);
        }

        PolarisGrpcServer server = new PolarisGrpcServer(this, context, this.builder.build());
        server.setDelayRegister(delayRegister);

        if (openGraceOffline) {
            server.setMaxWaitDuration(maxWaitDuration);
        }

        return server;
    }

    private void setDefault() {
        if (StringUtils.isBlank(namespace)) {
            this.namespace = DEFAULT_NAMESPACE;
        }
        if (heartbeatInterval == 0) {
            this.heartbeatInterval = DEFAULT_TTL;
        }
    }

    String getApplicationName() {
        return applicationName;
    }

    String getNamespace() {
        return namespace;
    }

    Map<String, String> getMetaData() {
        return metaData;
    }

    int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    String getHost() {
        return host;
    }

    SDKContext getContext() {
        return context;
    }

    boolean isOpenGraceOffline() {
        return openGraceOffline;
    }

    int getWeight() {
        return weight;
    }

    String getVersion() {
        return version;
    }
}
