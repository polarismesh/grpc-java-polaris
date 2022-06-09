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


package com.tencent.polaris.grpc.client;

import com.google.common.base.Preconditions;
import com.tencent.polaris.grpc.metadata.MetadataContext;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Contexts;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.MethodDescriptor;

import java.util.function.Predicate;

import static com.tencent.polaris.grpc.metadata.MetadataContext.METADATA_CONTEXT_KEY;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class MetadataClientInterceptor implements ClientInterceptor {

    private final Predicate<String> transitiveFilter;

    public MetadataClientInterceptor(Predicate<String> transitiveFilter) {
        Preconditions.checkNotNull(transitiveFilter, "transitiveFilter");
        this.transitiveFilter = transitiveFilter;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> methodDescriptor,
            CallOptions callOptions,
            Channel channel) {

        return new SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(methodDescriptor, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                copyMetadataToHeader(headers);
                super.start(responseListener, headers);
            }

        };
    }

    private void copyMetadataToHeader(Metadata headers) {
        MetadataContext metadataContext = METADATA_CONTEXT_KEY.get();

        metadataContext.getHeaderFragment().forEach((key, val) -> {
            if (!transitiveFilter.test(key)) {
                return;
            }
            headers.put(Key.of(key, Metadata.ASCII_STRING_MARSHALLER), val);
        });

        metadataContext.getGrpcContextFragment().forEach((key, val) -> {
            if (!transitiveFilter.test(key)) {
                return;
            }
            headers.put(Key.of(key, Metadata.ASCII_STRING_MARSHALLER), val);
        });
    }
}
