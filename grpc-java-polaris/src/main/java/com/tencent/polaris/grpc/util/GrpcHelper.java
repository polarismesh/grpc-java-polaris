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

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.ConnectivityState.READY;
import static io.grpc.ConnectivityState.SHUTDOWN;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.grpc.loadbalance.PolarisSubChannel;
import io.grpc.Attributes;
import io.grpc.ConnectivityStateInfo;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer.Subchannel;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class GrpcHelper {

    public static final Attributes.Key<Ref<ConnectivityStateInfo>> STATE_INFO =
            Attributes.Key.create("state-info");

    public static final class Ref<T> {

        T value;

        public Ref(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }

    public static <T> Set<T> setsDifference(Set<T> a, Set<T> b) {
        Set<T> aCopy = new HashSet<>(a);
        aCopy.removeAll(b);
        return aCopy;
    }

    public static Ref<ConnectivityStateInfo> getSubChannelStateInfoRef(
            Subchannel subchannel) {
        return checkNotNull(subchannel.getAttributes().get(STATE_INFO), "STATE_INFO");
    }

    static boolean isReady(Subchannel subchannel) {
        return getSubChannelStateInfoRef(subchannel).value.getState() == READY;
    }

    public static void shutdownSubChannel(Subchannel channel) {
        if (channel == null) {
            return;
        }

        channel.shutdown();
        getSubChannelStateInfoRef(channel).value =
                ConnectivityStateInfo.forNonError(SHUTDOWN);
    }

    public static Map<PolarisSubChannel, PolarisSubChannel> filterNonFailingSubChannels(
            Map<EquivalentAddressGroup, PolarisSubChannel> subChannels, AtomicReference<Attributes> attributeHolder) {
        Map<PolarisSubChannel, PolarisSubChannel> readySubChannels = new HashMap<>();

        subChannels.forEach((key, val) -> {
            if (isReady(val)) {
                attributeHolder.set(val.getAttributes());
                readySubChannels.put(val, val);
            }
        });

        return readySubChannels;
    }


    public static Map<String, String> collectLabels(Metadata headers, Predicate<String> predicate) {
        Map<String, String> labels = new HashMap<>();

        Metadata.AsciiMarshaller<String> marshaller = Metadata.ASCII_STRING_MARSHALLER;

        Set<String> keys = headers.keys();
        for (String key : keys) {
            if (predicate.test(key)) {
                Key<String> headerKey = Key.of(key, marshaller);
                if (headers.containsKey(headerKey)) {
                    labels.put(key.toLowerCase(), headers.get(headerKey));
                }
            }
        }

        return labels;
    }
}
