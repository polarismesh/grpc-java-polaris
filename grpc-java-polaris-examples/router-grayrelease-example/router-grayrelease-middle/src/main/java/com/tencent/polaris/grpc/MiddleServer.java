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
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class MiddleServer {

    public static void main(String[] args) {
        runGrpcServer(args);
    }

    private static void runGrpcServer(String[] args) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("env", args[0]);

        Server server = PolarisGrpcServerBuilder
                .forPort(0)
                .namespace("grayrelease")
                .host("127.0.0.1")
                .applicationName("MiddleServer")
                .metadata(metadata)
                .heartbeatInterval(5)
                .intercept(PolarisHelper.buildMetadataServerInterceptor())
                .addService(new MiddleProvider(metadata))
                .build();

        try {
            server.start();
            JvmHookHelper.addShutdownHook(() -> {
                long start = System.currentTimeMillis();
                System.out.println("start shutdown");
                server.shutdown();
                System.out.println("end shutdown, cost : " + (System.currentTimeMillis() - start) + "ms");
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
