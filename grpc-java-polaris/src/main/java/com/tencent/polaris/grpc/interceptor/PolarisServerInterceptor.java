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

package com.tencent.polaris.grpc.interceptor;

import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.grpc.server.PolarisGrpcServerBuilder;
import io.grpc.ServerInterceptor;

/**
 * server 侧的拦截器, 优先级优于 ServerInterceptor
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public abstract class PolarisServerInterceptor implements ServerInterceptor {

    /**
     * server 侧的拦截器，自动注入当前grpc-server与polaris有关的信息
     *
     * @param namespace 服务注册在polaris中的命名空间
     * @param applicationName 当前 grpc-server 进程对应的应用名称，如果调用 {@link PolarisGrpcServerBuilder#applicationName(String)}
     * @param context polaris-sdk的上下文，一个grpc-server进程复用同一个
     */
    public abstract void init(final String namespace, final String applicationName, final SDKContext context);

}
