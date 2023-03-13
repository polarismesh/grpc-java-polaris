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

package com.tencent.polaris.grpc.loadbalance;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.grpc.util.ClientCallInfo;
import io.grpc.ClientStreamTracer;
import io.grpc.Metadata;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * grpc 调用的 tracer 信息，记录每次 grpc 调用的情况
 * 1. 每次请求的相应时间
 * 2. 每次请求的结果，记录成功或者失败
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisClientStreamTracer extends ClientStreamTracer {

    private static final Logger LOG = LoggerFactory.getLogger(PolarisClientStreamTracer.class);

    private final ClientCallInfo info;

    private final Map<Integer, Long> perStreamRequestStartTimes = new ConcurrentHashMap<Integer, Long>();

    private final AtomicBoolean reported = new AtomicBoolean(false);

    private final ServiceCallResult result;

    public PolarisClientStreamTracer(StreamInfo info, Metadata headers, ClientCallInfo callInfo) {
        this.info = callInfo;
        this.result = new ServiceCallResult();

        this.result.setHost(callInfo.getInstance().getHost());
        this.result.setPort(callInfo.getInstance().getPort());
        this.result.setMethod(callInfo.getMethod());
        this.result.setNamespace(callInfo.getTargetNamespace());
        this.result.setService(callInfo.getTargetService());
    }

    @Override
    public void outboundMessage(int seqNo) {
        perStreamRequestStartTimes.put(seqNo, System.currentTimeMillis());
    }

    @Override
    public void inboundMessage(int seqNo) {
        Long startTime = perStreamRequestStartTimes.get(seqNo);
        if (Objects.isNull(startTime)) {
            return;
        }
        this.result.setRetStatus(RetStatus.RetSuccess);
        this.result.setRetCode(Status.OK.getCode().value());
        this.result.setDelay(System.currentTimeMillis() - startTime);

        try {
            this.info.getConsumerAPI().updateServiceCallResult(result);
        } catch (PolarisException e) {
            LOG.error("[grpc-polaris] do report invoke call ret fail in inboundMessageRead", e);
        }
    }

}
