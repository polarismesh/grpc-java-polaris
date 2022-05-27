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


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tencent.polaris.api.utils.StringUtils;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * @author lixiaoshuang
 */
@Slf4j
public class ConsumerServer {

    public static final HelloConsumer HELLO_CONSUMER = new HelloConsumer();

    public static void main(String[] args) throws Exception {
        log.info("http server start on port 40041");

        for (int i = 0; i < 10; i ++) {
            AtomicInteger holder = new AtomicInteger(i);
            Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
               try {
                   String response = ConsumerServer.HELLO_CONSUMER.hello("consumer-" + holder.get());
                   System.out.println("receive resp : " + response);
               } catch (Exception e) {
                   e.printStackTrace();
               }
            }, 200, 200, TimeUnit.MILLISECONDS);
        }

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(40041), 0);
        httpServer.createContext("/echo", new ConsumerHttpHandler());
        httpServer.setExecutor(Executors.newFixedThreadPool(10));
        httpServer.start();
    }
}

@Slf4j
class ConsumerHttpHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) {
        
        try {
            String paramStr = httpExchange.getRequestURI().getQuery();
            String param = "";
            if (StringUtils.isNotBlank(paramStr)) {
                String[] split = paramStr.split("=");
                param = split[1];
            }
            String response = ConsumerServer.HELLO_CONSUMER.hello(param);
            handleResponse(httpExchange, response);
        } catch (Exception ex) {
            log.error("ConsumerHttpHandler error", ex);
        }
    }
    
    /**
     * Processing response
     */
    private void handleResponse(HttpExchange httpExchange, String responseContent) throws Exception {
        log.info("");
        byte[] responseContentByte = responseContent.getBytes(StandardCharsets.UTF_8);
        
        httpExchange.getResponseHeaders().add("Content-Type:", "text/plain;charset=utf-8");
        
        httpExchange.sendResponseHeaders(200, responseContentByte.length);
        
        OutputStream out = httpExchange.getResponseBody();
        out.write(responseContentByte);
        out.flush();
        out.close();
    }
}
