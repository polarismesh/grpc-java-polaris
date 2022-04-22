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

import com.tencent.polaris.client.api.SDKContext;
import io.grpc.NameResolver;
import io.grpc.ProxiedSocketAddress;
import io.grpc.ProxyDetector;
import io.grpc.SynchronizationContext;

import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author lixiaoshuang
 */
public class PolarisNameResolverProviderTest {
    
    private PolarisNameResolverProvider polarisNameResolverProvider;
    
    @BeforeEach
    public void setUp() {
        polarisNameResolverProvider = new PolarisNameResolverProvider(SDKContext.initContext());
    }
    
    @Test
    public void testNewNameResolver() throws URISyntaxException {
        URI uri = new URI("polaris://grpc-demo-java");
        NameResolver.Args args = NameResolver.Args.newBuilder().setDefaultPort(8888)
                .setServiceConfigParser(new NameResolver.ServiceConfigParser() {
                    @Override
                    public NameResolver.ConfigOrError parseServiceConfig(Map<String, ?> rawServiceConfig) {
                        return null;
                    }
                }).setSynchronizationContext(new SynchronizationContext((t, e) -> {
                
                })).setProxyDetector(new ProxyDetector() {
                    @Nullable
                    @Override
                    public ProxiedSocketAddress proxyFor(SocketAddress targetServerAddress) {
                        return null;
                    }
                }).build();
        
        NameResolver nameResolver = polarisNameResolverProvider.newNameResolver(uri, args);
        assertNotNull(nameResolver);
    }
    
    @Test
    public void testIsAvailable() {
        assertTrue(polarisNameResolverProvider.isAvailable());
    }
    
    @Test
    public void testPriority() {
        int priority = polarisNameResolverProvider.priority();
        assertEquals(5, priority);
    }
    
    @Test
    public void testGetDefaultScheme() {
        String defaultScheme = polarisNameResolverProvider.getDefaultScheme();
        assertEquals("polaris", defaultScheme);
    }
}