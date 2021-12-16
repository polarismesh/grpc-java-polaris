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

import com.google.common.collect.Lists;
import com.tencent.polaris.grpc.server.PolarisGrpcServer;
import io.grpc.BindableService;

import java.util.List;

/**
 * @author lixiaoshuang
 */
public class ServerMain {

    public static void main(String[] args) {

        List<BindableService> services = Lists.newArrayList(new HelloProvider(),new HiProvider());

        PolarisGrpcServer polarisGrpcServer = PolarisGrpcServer.builder()
                .port(50051)
                .namespace("default")
                .applicationName("grpc-demo-java")
                .metaData(null)
                .ttl(5)
                .siteLocalIp("")
                .bindableServices(services)
                .build();

        polarisGrpcServer.start();

    }
}
