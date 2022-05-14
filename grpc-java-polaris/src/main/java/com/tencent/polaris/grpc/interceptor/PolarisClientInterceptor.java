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
import io.grpc.ClientInterceptor;

/**
 * client 侧的拦截器, 优先级优于 ClientInterceptor
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public abstract class PolarisClientInterceptor implements ClientInterceptor {

    /**
     * client 侧的拦截器，自动注入当前grpc-server与polaris有关的信息
     *
     * @param namespace 当前主调服务所在的命名空间
     * @param applicationName 当前主调服务的应用名称
     * @param context polaris-sdk的上下文信息，整个服务调用者进程一个
     */
    public abstract void init(final String namespace, final String applicationName, final SDKContext context);

}
