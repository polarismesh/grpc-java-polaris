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

import com.sun.net.httpserver.Headers;
import com.tencent.polaris.grpc.client.PolarisManagedChannelBuilder;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.stub.MetadataUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lixiaoshuang
 */
@Slf4j
public class HelloConsumer {

    private final Set<String> whiteHeaders = new HashSet<>();
    
    private final ManagedChannel channel;
    
    public HelloConsumer() {
        channel = PolarisManagedChannelBuilder.forTarget("polaris://RouterServerGRPCJava")
                .usePlaintext()
                .build();

        whiteHeaders.add("env");
    }
    
    public String hello(String value, Headers headers) {
        Metadata metadata = new Metadata();
        headers.forEach((s, val) -> {
            if (whiteHeaders.contains(s.toLowerCase())) {
                metadata.put(Key.of(s, Metadata.ASCII_STRING_MARSHALLER), val.get(0));
            }
        });

        HelloGrpc.HelloBlockingStub helloBlockingStub = HelloGrpc.newBlockingStub(channel);
        helloBlockingStub = helloBlockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
        HelloPolaris.request request = HelloPolaris.request.newBuilder().setMsg(value).build();
        HelloPolaris.response response = helloBlockingStub.sayHello(request);
        System.out.println("grpc server response ---------> :" + response.getData());
        return response.getData();
    }
}
