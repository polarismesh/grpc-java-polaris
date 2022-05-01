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

import com.google.common.base.Preconditions;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.grpc.util.ClientCallInfo;
import com.tencent.polaris.grpc.util.Common;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceResponse;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import io.grpc.Attributes;
import io.grpc.LoadBalancer.Helper;
import io.grpc.LoadBalancer.PickResult;
import io.grpc.LoadBalancer.PickSubchannelArgs;
import io.grpc.LoadBalancer.Subchannel;
import io.grpc.LoadBalancer.SubchannelPicker;
import io.grpc.Status;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisPicker extends SubchannelPicker {

    private final List<Subchannel> channels;

    private final ConsumerAPI consumerAPI;

    private final Helper helper;

    public PolarisPicker(final List<Subchannel> channels, final ConsumerAPI consumerAPI, final Helper helper) {
        this.channels = channels;
        this.helper = helper;
        this.consumerAPI = consumerAPI;
    }

    @Override
    public PickResult pickSubchannel(PickSubchannelArgs args) {
        if (channels.isEmpty()) {
            return PickResult.withNoResult();
        }

        Attributes attributes = channels.get(0).getAddresses().getAttributes();
        final String targetNamespace = attributes.get(Common.TARGET_NAMESPACE_KEY);
        final String targetService = attributes.get(Common.TARGET_SERVICE_KEY);

        Map<Instance, Subchannel> instances = channels.stream()
                .collect(HashMap::new,
                        ((hm, channel) -> hm.put(channel.getAttributes().get(Common.INSTANCE_KEY), channel)),
                        HashMap::putAll);

        try {
            final GetOneInstanceRequest request = new GetOneInstanceRequest();
            request.setNamespace(targetNamespace);
            request.setService(targetService);
            InstancesResponse response = consumerAPI.getOneInstance(request);
            Instance instance = response.getInstances()[0];
            Subchannel channel = instances.get(instance);
            return channel == null ? PickResult.withNoResult() : PickResult.withSubchannel(channel,
                    new PolarisClientStreamTracerFactory(ClientCallInfo.builder()
                            .consumerAPI(consumerAPI)
                            .instance(instance)
                            .targetNamespace(targetNamespace)
                            .targetService(targetService)
                            .method(args.getMethodDescriptor().getBareMethodName())
                            .build()));
        } catch (PolarisException e) {
            return PickResult.withError(Status.UNKNOWN.withCause(e));
        }

    }

    public static final class EmptyPicker extends SubchannelPicker  {

        private final Status status;

        EmptyPicker(Status status) {
            this.status = Preconditions.checkNotNull(status, "status");
        }

        public PickResult pickSubchannel(PickSubchannelArgs args) {
            return this.status.isOk() ? PickResult.withNoResult() : PickResult.withError(this.status);
        }
    }
}
