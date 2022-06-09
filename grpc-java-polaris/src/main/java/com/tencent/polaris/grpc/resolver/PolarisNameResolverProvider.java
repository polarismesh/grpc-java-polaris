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

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.grpc.exception.PolarisGrpcException;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service provider class
 *
 * @author lixiaoshuang
 */
public class PolarisNameResolverProvider extends NameResolverProvider {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PolarisNameResolverProvider.class);
    
    private static final int DEFAULT_PRIORITY = 5;
    
    private static final String DEFAULT_SCHEME = "polaris";

    private static final String PATTERN = "polaris://[a-zA-Z0-9_:.-]{1,128}";

    private final SDKContext context;

    private final ConsumerAPI consumerAPI;

    public PolarisNameResolverProvider(final SDKContext context) {
        this.context = context;
        this.consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(context);
    }

    /**
     * Creates a NameResolver for the given target URI.
     *
     * @param targetUri the target URI to be resolved, whose scheme must not be null
     * @param args      other information that may be useful
     * @return NameResolver
     */
    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        if (!DEFAULT_SCHEME.equals(targetUri.getScheme())) {
            return null;
        }
        return new PolarisNameResolver(targetUri, context, consumerAPI);
    }
    
    /**
     * service is available.
     *
     * @return isAvailable
     */
    @Override
    protected boolean isAvailable() {
        return true;
    }
    
    /**
     * Default priority 5.
     *
     * @return priority
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
