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

import com.tencent.polaris.grpc.client.PolarisManagedChannelBuilder;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lixiaoshuang
 */
@Slf4j
public class HelloConsumer {
    
    private ManagedChannel channel;
    
    public HelloConsumer() {
        channel = PolarisManagedChannelBuilder.forTarget("polaris://DiscoverServerGRPCJava")
                .usePlaintext()
                .build();
    }
    
    public String hello(String value) {
        HelloGrpc.HelloBlockingStub helloBlockingStub = HelloGrpc.newBlockingStub(channel);
        HelloPolaris.request request = HelloPolaris.request.newBuilder().setMsg(value).build();
        HelloPolaris.response response = helloBlockingStub.sayHello(request);
        System.out.println("grpc server response ---------> :" + response.getData());
        return response.getData();
    }
}
