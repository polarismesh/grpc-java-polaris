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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;


/**
 * @author lixiaoshuang
 */
public class IpUtil {
    
    private static final Logger log = LoggerFactory.getLogger(IpUtil.class);
    
    private static final String LOCALHOST_VALUE = "127.0.0.1";
    
    /**
     * 获取本机ip
     *
     * @return
     */
    public static String getLocalHost() {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (Throwable e) {
            log.error(e.getMessage());
        }
        if (inetAddress == null) {
            return LOCALHOST_VALUE;
        }
        return inetAddress.getHostAddress();
    }
}
