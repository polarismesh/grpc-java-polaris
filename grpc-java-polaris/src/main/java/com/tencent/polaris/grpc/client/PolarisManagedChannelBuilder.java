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

package com.tencent.polaris.grpc.client;


import com.google.common.annotations.VisibleForTesting;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.grpc.interceptor.PolarisClientInterceptor;
import com.tencent.polaris.grpc.loadbalance.PolarisLoadBalancerFactory;
import com.tencent.polaris.grpc.resolver.PolarisNameResolverFactory;
import com.tencent.polaris.grpc.util.JvmHookHelper;
import io.grpc.BinaryLog;
import io.grpc.ClientInterceptor;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver.Factory;
import io.grpc.ProxyDetector;
import shade.polaris.com.google.gson.Gson;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tencent.polaris.grpc.loadbalance.PolarisLoadBalancerProvider.LOADBALANCER_PROVIDER;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisManagedChannelBuilder extends ManagedChannelBuilder<PolarisManagedChannelBuilder> {

    private static final AtomicBoolean FIRST_INIT = new AtomicBoolean(false);
    private static final Object MONITOR = new Object();

    private static volatile SDKContext CONTEXT;

    private final ManagedChannelBuilder<?> builder;

    private final List<PolarisClientInterceptor> polarisInterceptors = new ArrayList<>();

    private final List<ClientInterceptor> interceptors = new ArrayList<>();

    private final ServiceKey sourceService;

    /**
     * follow {@link ManagedChannelBuilder#forTarget(String)}
     *
     * @param target 服务名
     * @return {@link PolarisManagedChannelBuilder}
     */
    public static PolarisManagedChannelBuilder forTarget(String target) {
        return forTarget(target, null, null);
    }

    /**
     * 增强 {@link ManagedChannelBuilder#forTarget(String)}, 在连接到目标服务时允许设置主调服务的相关信息
     *
     * @param target        服务名
     * @param sourceService {@link ServiceKey} 主调服务信息以及标签
     * @return {@link PolarisManagedChannelBuilder}
     */
    public static PolarisManagedChannelBuilder forTarget(String target, ServiceKey sourceService) {
        return new PolarisManagedChannelBuilder(target, sourceService, null);
    }

    /**
     * 增强 {@link ManagedChannelBuilder#forTarget(String)}, 在连接到目标服务时允许设置主调服务的相关信息, 并且可以自定义北极星 SDK 的核心数据结构 {@link SDKContext}
     *
     * @param target        服务名
     * @param sourceService {@link ServiceKey} 主调服务信息以及标签
     * @param sdkContext    {@link SDKContext} 可以设置北极星 SDK 的相关配置以及行为, 例如服务治理中心地址等等
     * @return {@link PolarisManagedChannelBuilder}
     */
    public static PolarisManagedChannelBuilder forTarget(String target, ServiceKey sourceService, SDKContext sdkContext) {
        return new PolarisManagedChannelBuilder(target, sourceService, sdkContext);
    }

    private PolarisManagedChannelBuilder(String target, ServiceKey sourceService, SDKContext context) {
        if (FIRST_INIT.compareAndSet(false, true)) {
            if (context == null) {
                context = SDKContext.initContext();
                JvmHookHelper.addShutdownHook(context::destroy);
            }
            PolarisLoadBalancerFactory.init(context);
            PolarisNameResolverFactory.init(context);
            CONTEXT = context;
            synchronized (MONITOR) {
                MONITOR.notifyAll();
            }
        }
        SDKContext prev = CONTEXT;
        if (prev == null) {
            synchronized (MONITOR) {
                try {
                    MONITOR.wait();
                } catch (InterruptedException e) {
                    //noop
                }
                prev = CONTEXT;
            }
        }
        if (context != null && prev != context) {
            throw new IllegalStateException("[Polaris] SDKContext already initialize");
        }

        this.builder = ManagedChannelBuilder.forTarget(buildUrl(target, sourceService));
        this.sourceService = sourceService;
    }

    public static SDKContext getSDKContext() {
        return CONTEXT;
    }

    @VisibleForTesting
    static void resetSDKContext() {
        CONTEXT = null;
    }

    public PolarisManagedChannelBuilder directExecutor() {
        builder.directExecutor();
        return this;
    }

    public PolarisManagedChannelBuilder executor(Executor executor) {
        builder.executor(executor);
        return this;
    }

    public PolarisManagedChannelBuilder intercept(List<ClientInterceptor> interceptors) {

        for (ClientInterceptor interceptor : interceptors) {
            if (interceptor instanceof PolarisClientInterceptor) {
                this.polarisInterceptors.add((PolarisClientInterceptor) interceptor);
            } else {
                this.interceptors.add(interceptor);
            }
        }
        return this;
    }

    public PolarisManagedChannelBuilder intercept(ClientInterceptor... interceptors) {
        for (ClientInterceptor interceptor : interceptors) {
            if (interceptor instanceof PolarisClientInterceptor) {
                this.polarisInterceptors.add((PolarisClientInterceptor) interceptor);
            } else {
                this.interceptors.add(interceptor);
            }
        }
        return this;
    }


    public PolarisManagedChannelBuilder userAgent(String userAgent) {
        builder.userAgent(userAgent);
        return this;
    }

    public PolarisManagedChannelBuilder overrideAuthority(String authority) {
        builder.overrideAuthority(authority);
        return this;
    }

    @Deprecated
    public PolarisManagedChannelBuilder nameResolverFactory(Factory resolverFactory) {
        builder.nameResolverFactory(resolverFactory);
        return this;
    }

    public PolarisManagedChannelBuilder decompressorRegistry(DecompressorRegistry registry) {
        builder.decompressorRegistry(registry);
        return this;
    }

    public PolarisManagedChannelBuilder compressorRegistry(CompressorRegistry registry) {
        builder.compressorRegistry(registry);
        return this;
    }

    public PolarisManagedChannelBuilder idleTimeout(long value, TimeUnit unit) {
        builder.idleTimeout(value, unit);
        return this;
    }

    public PolarisManagedChannelBuilder offloadExecutor(Executor executor) {
        this.builder.offloadExecutor(executor);
        return this;
    }

    public PolarisManagedChannelBuilder usePlaintext() {
        this.builder.usePlaintext();
        return this;
    }

    public PolarisManagedChannelBuilder useTransportSecurity() {
        this.builder.useTransportSecurity();
        return this;
    }

    @Deprecated
    public PolarisManagedChannelBuilder enableFullStreamDecompression() {
        return this;
    }

    public PolarisManagedChannelBuilder maxInboundMessageSize(int bytes) {
        this.builder.maxInboundMessageSize(bytes);
        return this;
    }

    public PolarisManagedChannelBuilder maxInboundMetadataSize(int bytes) {
        this.builder.maxInboundMetadataSize(bytes);
        return this;
    }

    public PolarisManagedChannelBuilder keepAliveTime(long keepAliveTime, TimeUnit timeUnit) {
        this.builder.keepAliveTime(keepAliveTime, timeUnit);
        return this;
    }

    public PolarisManagedChannelBuilder keepAliveTimeout(long keepAliveTimeout, TimeUnit timeUnit) {
        this.builder.keepAliveTimeout(keepAliveTimeout, timeUnit);
        return this;
    }

    public PolarisManagedChannelBuilder keepAliveWithoutCalls(boolean enable) {
        this.builder.keepAliveWithoutCalls(enable);
        return this;
    }

    public PolarisManagedChannelBuilder maxRetryAttempts(int maxRetryAttempts) {
        this.builder.maxRetryAttempts(maxRetryAttempts);
        return this;
    }

    public PolarisManagedChannelBuilder maxHedgedAttempts(int maxHedgedAttempts) {
        this.builder.maxHedgedAttempts(maxHedgedAttempts);
        return this;
    }

    public PolarisManagedChannelBuilder retryBufferSize(long bytes) {
        this.builder.retryBufferSize(bytes);
        return this;
    }

    public PolarisManagedChannelBuilder perRpcBufferLimit(long bytes) {
        this.builder.perRpcBufferLimit(bytes);
        return this;
    }

    public PolarisManagedChannelBuilder disableRetry() {
        this.builder.disableRetry();
        return this;
    }

    public PolarisManagedChannelBuilder enableRetry() {
        this.builder.enableRetry();
        return this;
    }

    public PolarisManagedChannelBuilder setBinaryLog(BinaryLog binaryLog) {
        this.builder.setBinaryLog(binaryLog);
        return this;
    }

    public PolarisManagedChannelBuilder maxTraceEvents(int maxTraceEvents) {
        this.builder.maxTraceEvents(maxTraceEvents);
        return this;
    }

    public PolarisManagedChannelBuilder proxyDetector(ProxyDetector proxyDetector) {
        this.builder.proxyDetector(proxyDetector);
        return this;
    }

    public PolarisManagedChannelBuilder defaultServiceConfig(@Nullable Map<String, ?> serviceConfig) {
        this.builder.defaultServiceConfig(serviceConfig);
        return this;
    }

    public PolarisManagedChannelBuilder disableServiceConfigLookUp() {
        this.builder.disableServiceConfigLookUp();
        return this;
    }

    public ManagedChannel build() {
        for (PolarisClientInterceptor clientInterceptor : polarisInterceptors) {
            clientInterceptor.init(this.sourceService.getNamespace(), this.sourceService.getService(), CONTEXT);
            this.builder.intercept(clientInterceptor);
        }
        this.builder.intercept(interceptors);
        this.builder.defaultLoadBalancingPolicy(LOADBALANCER_PROVIDER);
        return builder.build();
    }

    private static String buildUrl(String target, ServiceKey sourceService) {
        if (Objects.isNull(sourceService)) {
            return target;
        }

        String extendInfo = Base64.getUrlEncoder().encodeToString(new Gson().toJson(sourceService)
                .getBytes(StandardCharsets.UTF_8));

        if (target.contains("?")) {
            target += "&extend_info=" + extendInfo;
        } else {
            target += "?extend_info=" + extendInfo;
        }

        return target;
    }
}