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
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.grpc.util.JvmShutdownHookUtil;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service provider class
 *
 * @author lixiaoshuang
 */
public class PolarisNameResolverProvider extends NameResolverProvider {
    
    private final Logger log = LoggerFactory.getLogger(PolarisNameResolverProvider.class);
    
    private static final int DEFAULT_PRIORITY = 5;
    
    private static final String DEFAULT_SCHEME = "polaris";
    
    private static final String PATTERN = "polaris://[a-zA-Z0-9_:.-]{1,128}";
    
    private final ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPI();
    
    
    public PolarisNameResolverProvider() {
        JvmShutdownHookUtil.addHook(consumerAPI::destroy);
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
        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(targetUri.toString());
        if (!matcher.matches()) {
            log.error("target format is wrong,reference: polaris://[serviceName]");
            return null;
        }
        return new PolarisNameResolver(targetUri, consumerAPI);
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
