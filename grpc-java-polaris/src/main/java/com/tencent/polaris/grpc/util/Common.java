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

package com.tencent.polaris.grpc.util;

import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import io.grpc.Attributes.Key;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class Common {

    public static final String GRPC_HEADER_PREFIX = "grpc.header.";

    public static final String GRPC_CONTEXT_PREFIX = "grpc.ctx.";

    public static final String GRPC_SYSTEM_ENV_PREFIX = "sys.env.";


    public static final int GRPC_HEADER_PREFIX_LEN = GRPC_HEADER_PREFIX.length();

    public static final int GRPC_CONTEXT_PREFIX_LEN = GRPC_CONTEXT_PREFIX.length();

    public static final int GRPC_SYSTEM_ENV_PREFIX_LEN = GRPC_SYSTEM_ENV_PREFIX.length();

    /**
     * {@link io.grpc.Attributes} 中存放 {@link Instance} 的 key
     */
    public static final Key<Instance> INSTANCE_KEY = Key.create(Instance.class.getName());

    /**
     *
     */
    public static final Key<ServiceInfo> SOURCE_SERVICE_INFO = Key.create(ServiceInfo.class.getName());

    /**
     * {@link io.grpc.Attributes} 中存放服务调用者的服务名称信息
     */
    public static final Key<String> TARGET_SERVICE_KEY = Key.create("POLARIS_SOURCE_SERVICE");

    /**
     * {@link io.grpc.Attributes} 中存放服务调用者所在的命名空间信息
     */
    public static final Key<String> TARGET_NAMESPACE_KEY = Key.create("POLARIS_SOURCE_NAMESPACE");

}
