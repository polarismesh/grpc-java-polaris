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

import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import java.net.URI;

/**
 * Service provider class
 *
 * @author lixiaoshuang
 */
public class PolarisNameResolverProvider extends NameResolverProvider {
    
    private static final int DEFAULT_PRIORITY = 5;
    
    private static final String DEFAULT_SCHEME = "polaris";
    
    private final String namespace;
    
    private final String service;
    
    
    public PolarisNameResolverProvider(String namespace, String service) {
        this.namespace = namespace;
        this.service = service;
    }
    
    /**
     * Create service discovery class
     *
     * @param targetUri
     * @param args
     * @return
     */
    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        return new PolarisNameResolver(namespace, service);
    }
    
    /**
     * service is available
     *
     * @return
     */
    @Override
    protected boolean isAvailable() {
        return true;
    }
    
    /**
     * Default priority 5
     *
     * @return
     */
    @Override
    protected int priority() {
        return DEFAULT_PRIORITY;
    }
    
    
    @Override
    public String getDefaultScheme() {
        return DEFAULT_SCHEME;
    }
}
