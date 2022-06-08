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

import com.tencent.polaris.grpc.server.PolarisGrpcServerBuilder;

import com.tencent.polaris.grpc.util.JvmHookHelper;
import com.tencent.polaris.grpc.util.PolarisHelper;
import io.grpc.Server;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lixiaoshuang
 */
public class ServerMain {

    public static void main(String[] args) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("env", args[0]);

        Server polarisGrpcServer = PolarisGrpcServerBuilder
                .forPort(0)
                .namespace("default")
                .applicationName("RouterServerGRPCJava")
                .metadata(metadata)
                .heartbeatInterval(5)
                .addService(new HelloProvider(metadata))
                .addService(new HiProvider())
                .build();

        try {
            Server server = polarisGrpcServer.start();
            JvmHookHelper.addShutdownHook(() -> {
                long start = System.currentTimeMillis();
                System.out.println("start shutdown");
                server.shutdown();
                System.out.println("end shutdown, cost : " + (System.currentTimeMillis() - start) + "ms");
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
