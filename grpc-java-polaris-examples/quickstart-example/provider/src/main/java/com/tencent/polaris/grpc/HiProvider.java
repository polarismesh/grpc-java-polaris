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

import io.grpc.stub.StreamObserver;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author lixiaoshuang
 */
public class HiProvider extends HiGrpc.HiImplBase {

    String[] args;

    public HiProvider(String[] args) {
        this.args = args;
    }
    
    @Override
    public void sayHi(HelloPolaris.request request, StreamObserver<HelloPolaris.response> responseObserver) {
        String msg = "I'm DiscoverServerGRPCJava provider, "
                + "My Info : "
                + Arrays.toString(args)
                + request.getMsg();
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
        HelloPolaris.response response = HelloPolaris.response.newBuilder().setData(msg).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
