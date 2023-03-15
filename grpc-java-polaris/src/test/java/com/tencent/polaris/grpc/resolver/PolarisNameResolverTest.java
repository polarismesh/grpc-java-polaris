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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import org.junit.Test;

import java.net.URI;

/**
 * @author lixiaoshuang
 */
public class PolarisNameResolverTest {

    @Test
    public void testGetServiceAuthority() throws Exception {
        SDKContext context = SDKContext.initContext();
        URI targetUri = new URI("polaris://grpc-demo-java?namespace=default");
        PolarisNameResolver polarisNameResolver = new PolarisNameResolver(targetUri, context, DiscoveryAPIFactory.createConsumerAPIByContext(context));
        String serviceAuthority = polarisNameResolver.getServiceAuthority();
        assertNotNull(serviceAuthority);
        assertEquals("grpc-demo-java", serviceAuthority);
    }

}