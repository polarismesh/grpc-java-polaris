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

import com.tencent.polaris.grpc.metadata.MetadataContext;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import io.grpc.internal.GrpcUtil;
import java.util.Set;

import static com.tencent.polaris.grpc.metadata.MetadataContext.METADATA_CONTEXT_KEY;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class MetadataServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall,
            Metadata metadata,
            ServerCallHandler<ReqT, RespT> next) {

        Context newCtx = copyMetadataToMetadataContext(metadata);

        return Contexts.interceptCall(newCtx, serverCall, metadata, next);
    }

    private Context copyMetadataToMetadataContext(Metadata headers) {
        MetadataContext metadataContext = METADATA_CONTEXT_KEY.get();
        metadataContext.reset();

        Set<String> keys = headers.keys();

        for (String key : keys) {
            String val = headers.get(Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
            metadataContext.putHeaderFragment(key, val);
        }

        return Context.current().withValue(METADATA_CONTEXT_KEY, metadataContext);
    }
}
