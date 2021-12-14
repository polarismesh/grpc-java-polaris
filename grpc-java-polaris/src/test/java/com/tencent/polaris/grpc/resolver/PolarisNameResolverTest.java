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
import io.grpc.Attributes;
import io.grpc.NameResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

/**
 * @author lixiaoshuang
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisNameResolverTest {
    
    private PolarisNameResolver polarisNameResolver;
    
    @Mock
    private ConsumerAPI consumerAPI;
    
    @Mock
    private NameResolver.Listener listener;
    
    
    @Before
    public void setUp() throws URISyntaxException {
        URI targetUri = new URI("polaris://grpc-demo-java?namespace=default");
        polarisNameResolver = new PolarisNameResolver(targetUri, consumerAPI);
    }
    
    @Test
    public void testGetServiceAuthority() {
        String serviceAuthority = polarisNameResolver.getServiceAuthority();
        assertNotNull(serviceAuthority);
    }
    
    @Test
    public void testStart() {
        polarisNameResolver.start(listener);
        Mockito.verify(listener).onAddresses(null, Attributes.EMPTY);
    }
}