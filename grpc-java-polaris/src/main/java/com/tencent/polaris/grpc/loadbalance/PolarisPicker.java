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
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.grpc.util.Common;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceResponse;
import io.grpc.LoadBalancer.PickResult;
import io.grpc.LoadBalancer.PickSubchannelArgs;
import io.grpc.LoadBalancer.Subchannel;
import io.grpc.LoadBalancer.SubchannelPicker;
import io.grpc.Status;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisPicker extends SubchannelPicker {

    private final Collection<Subchannel> channels;

    private final RouterAPI routerAPI;

    private final String rule;

    public PolarisPicker(final Collection<Subchannel> channels, final String rule, final RouterAPI routerAPI) {
        this.channels = channels;
        this.routerAPI = routerAPI;
        this.rule = rule;
    }

    @Override
    public PickResult pickSubchannel(PickSubchannelArgs args) {
        Map<Instance, Subchannel> instances = channels.stream()
                .collect(HashMap::new,
                        ((hm, channel) -> hm.put(channel.getAttributes().get(Common.INSTANCE_KEY), channel)),
                        HashMap::putAll);
        final ProcessLoadBalanceRequest request = new ProcessLoadBalanceRequest();
        final Criteria criteria = new Criteria();
        criteria.setHashKey(args.getMethodDescriptor().getFullMethodName());
        request.setLbPolicy(rule);
        request.setCriteria(criteria);
        request.setDstInstances(new DefaultServiceInstances(null, new ArrayList<>(instances.keySet())));
        ProcessLoadBalanceResponse response = routerAPI.processLoadBalance(request);
        final Instance target = response.getTargetInstance();

        Subchannel channel = instances.get(target);
        return channel == null ? PickResult.withNoResult() : PickResult.withSubchannel(channel);
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
