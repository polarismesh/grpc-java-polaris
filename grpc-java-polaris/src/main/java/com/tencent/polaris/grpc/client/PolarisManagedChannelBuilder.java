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

import static com.tencent.polaris.grpc.loadbalance.PolarisLoadBalancerProvider.LOADBALANCER_PROVIDER;

import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.grpc.loadbalance.PolarisLoadBalancerFactory;
import com.tencent.polaris.grpc.resolver.PolarisNameResolverFactory;
import io.grpc.ClientInterceptor;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver.Factory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisManagedChannelBuilder extends ManagedChannelBuilder<PolarisManagedChannelBuilder> {

    private final ManagedChannelBuilder<?> builder;

    private String balanceRule;

    private final List<PolarisClientInterceptor> polarisInterceptors = new ArrayList<>();

    private final List<ClientInterceptor> interceptors = new ArrayList<>();

    public PolarisManagedChannelBuilder(ManagedChannelBuilder<?> builder) {
        this.builder = builder;
    }

    public static ManagedChannelBuilder<PolarisManagedChannelBuilder> forTarget(String target) {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forTarget(target);
        return new PolarisManagedChannelBuilder(builder);
    }

    public static ManagedChannelBuilder<?> forAddress(String name, int port) {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(name, port);
        return new PolarisManagedChannelBuilder(builder);
    }

    @Override
    public PolarisManagedChannelBuilder directExecutor() {
         builder.directExecutor();
         return this;
    }

    @Override
    public PolarisManagedChannelBuilder executor(Executor executor) {
         builder.executor(executor);
         return this;
    }

    @Override
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

    @Override
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

    @Override
    public PolarisManagedChannelBuilder userAgent(String userAgent) {
        builder.userAgent(userAgent);
        return this;
    }

    @Override
    public PolarisManagedChannelBuilder overrideAuthority(String authority) {
        builder.overrideAuthority(authority);
        return this;
    }

    @Override
    @Deprecated
    public PolarisManagedChannelBuilder nameResolverFactory(Factory resolverFactory) {
        builder.nameResolverFactory(resolverFactory);
        return this;
    }

    @Override
    public PolarisManagedChannelBuilder decompressorRegistry(DecompressorRegistry registry) {
        builder.decompressorRegistry(registry);
        return this;
    }

    @Override
    public PolarisManagedChannelBuilder compressorRegistry(CompressorRegistry registry) {
        builder.compressorRegistry(registry);
        return this;
    }

    @Override
    public PolarisManagedChannelBuilder idleTimeout(long value, TimeUnit unit) {
        builder.idleTimeout(value, unit);
        return this;
    }

    public PolarisManagedChannelBuilder loadBalanceRule(String rule) {
        this.balanceRule = rule;
        return this;
    }

    @Override
    public ManagedChannel build() {
        SDKContext context = SDKContext.initContext();
        PolarisLoadBalancerFactory.init(context, this.balanceRule);
        PolarisNameResolverFactory.init(context);

        for (PolarisClientInterceptor clientInterceptor : polarisInterceptors) {
            this.builder.intercept(clientInterceptor);
        }
        this.builder.intercept(interceptors);
        this.builder.defaultLoadBalancingPolicy(LOADBALANCER_PROVIDER);
        return builder.build();
    }
}
