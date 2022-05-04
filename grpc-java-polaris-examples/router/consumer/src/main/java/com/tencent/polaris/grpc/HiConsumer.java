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
import com.tencent.polaris.grpc.resolver.PolarisNameResolverProvider;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lixiaoshuang
 */
@Slf4j
public class HiConsumer {
    
    public static void main(String[] args) {
        
        ManagedChannel channel = PolarisManagedChannelBuilder.forTarget("polaris://RouterServerGRPCJava").usePlaintext()
                .build();
        
        HiGrpc.HiBlockingStub hiBlockingStub = HiGrpc.newBlockingStub(channel);
        HelloPolaris.request request = HelloPolaris.request.newBuilder().setMsg("hi polaris").build();
        HelloPolaris.response response = hiBlockingStub.sayHi(request);
        System.out.println("grpc server response ---------> :" + response.getData());
        System.exit(1);
    }
}
