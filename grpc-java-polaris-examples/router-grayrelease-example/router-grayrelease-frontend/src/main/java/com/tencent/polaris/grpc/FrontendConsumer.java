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

package com.tencent.polaris.grpc;

import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.grpc.client.PolarisManagedChannelBuilder;
import com.tencent.polaris.grpc.util.PolarisHelper;
import io.grpc.ManagedChannel;

import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lixiaoshuang
 */
@Slf4j
public class FrontendConsumer {

    private final Set<String> whiteHeaders = new HashSet<>();

    private final ManagedChannel channel;
    
    public FrontendConsumer() {
        final ServiceInfo sourceService = new ServiceInfo();
        sourceService.setNamespace("grayrelease");
        sourceService.setService("FrontendServer");
        channel = PolarisManagedChannelBuilder.forTarget("polaris://MiddleServer?namespace=grayrelease", sourceService)
                .usePlaintext()
                .intercept(PolarisHelper.buildMetadataClientInterceptor())
                .build();

        whiteHeaders.add("env");
    }
    
    public String hello(String value) {
        HelloGrpc.HelloBlockingStub helloBlockingStub = HelloGrpc.newBlockingStub(channel);
        HelloPolaris.request request = HelloPolaris.request.newBuilder().setMsg(value).build();
        HelloPolaris.response response = helloBlockingStub.sayHello(request);
        return response.getData();
    }
}
