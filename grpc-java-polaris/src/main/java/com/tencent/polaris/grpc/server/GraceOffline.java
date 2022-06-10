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

package com.tencent.polaris.grpc.server;

import io.grpc.Server;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public final class GraceOffline {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraceOffline.class);

    private final Server grpcServer;

    private final Duration maxWaitDuration;

    private final AtomicBoolean executed = new AtomicBoolean(false);

    public GraceOffline(Server server, Duration maxWaitDuration) {
        this.grpcServer = server;
        this.maxWaitDuration = maxWaitDuration;
    }

    public Server shutdown() {
        if (!executed.compareAndSet(false, true)) {
            return grpcServer;
        }
        LOGGER.info("[grpc-polaris] begin grace shutdown");
        grpcServer.shutdown();

        try {
            // 等待 4 个 pull 时间间隔
            TimeUnit.SECONDS.sleep(4 * 2);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }

        try {
            grpcServer.awaitTermination(maxWaitDuration.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
        return grpcServer;
    }

}
