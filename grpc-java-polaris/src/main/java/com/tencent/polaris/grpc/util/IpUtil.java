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

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;


/**
 * @author lixiaoshuang
 */
public class IpUtil {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(IpUtil.class);
    
    private static final String LOCALHOST_VALUE = "127.0.0.1";

    public static String getLocalHost(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            InetAddress address = socket.getLocalAddress();
            return address.getHostAddress();
        } catch (IOException ex) {
            LOGGER.error("getLocalHost through socket fail : {}", ex.getMessage());
            return getLocalHostExactAddress();
        }
    }
    
    /**
     * Get real local ip
     * <p>
     * You can use getNetworkInterfaces()+getInetAddresses() to get all the IP addresses of the node, and then judge to
     * find out the site-local address, this is a recommended solution
     *
     * @return real ip
     */
    public static String getLocalHostExactAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface iface = networkInterfaces.nextElement();
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            return inetAddr.getHostAddress();
                        }
                    }
                }
            }
            return getLocalHost();
        } catch (Exception e) {
            LOGGER.error("getLocalHostExactAddress error", e);
        }
        return null;
    }
    
    /**
     * Get local ip
     * <p>
     * There are environmental restrictions. Different environments may get different ips.
     */
    public static String getLocalHost() {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage());
        }
        if (inetAddress == null) {
            return LOCALHOST_VALUE;
        }
        return inetAddress.getHostAddress();
    }
}
