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
import io.grpc.BindableService;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.HandlerRegistry;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public final class PolarisGrpcServerBuilder extends ServerBuilder<PolarisGrpcServerBuilder> {

    private String applicationName;

    private String namespace;

    private Map<String, String> metaData = new HashMap<>();

    private int ttl;

    private String host;

    private static final String DEFAULT_NAMESPACE = "default";

    private static final int DEFAULT_TTL = 5;

    private final ServerBuilder<?> builder;

    public static PolarisGrpcServerBuilder forPort(int port) {
        ServerBuilder<?> builder = ServerBuilder.forPort(port);
        return new PolarisGrpcServerBuilder(builder);
    }

    public PolarisGrpcServerBuilder(ServerBuilder<?> builder) {
        this.builder = builder;
    }

    public PolarisGrpcServerBuilder applicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    public PolarisGrpcServerBuilder namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public PolarisGrpcServerBuilder metaData(Map<String, String> metadata) {
        this.metaData = metadata;
        return this;
    }

    public PolarisGrpcServerBuilder ttl(int ttl) {
        this.ttl = ttl;
        return this;
    }

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
    public Server build() {
        checkField();
        return new PolarisGrpcServer(this, this.builder.build());
    }

    private void checkField() {
        if (StringUtils.isBlank(namespace)) {
            this.namespace = DEFAULT_NAMESPACE;
        }
        if (ttl == 0) {
            this.ttl = DEFAULT_TTL;
        }
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getNamespace() {
        return namespace;
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }

    public int getTtl() {
        return ttl;
    }

    public String getHost() {
        return host;
    }

    public ServerBuilder<?> getBuilder() {
        return builder;
    }
}
