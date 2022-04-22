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

import static io.grpc.ConnectivityState.READY;
import static io.grpc.ConnectivityState.TRANSIENT_FAILURE;

import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.RouterAPIFactory;
import com.tencent.polaris.router.api.core.RouterAPI;
import io.grpc.ConnectivityState;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.Status;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import shade.polaris.com.google.common.base.Preconditions;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisLoadBalancer extends LoadBalancer {

    private RouterAPI routerAPI;

    private Helper helper;

    private ConnectivityState currentState;

    private PolarisPicker currentPicker;

    private static final Status EMPTY_OK = Status.OK.withDescription("no subChannels ready");

    private final Map<EquivalentAddressGroup, Subchannel> subChannels = new ConcurrentHashMap<>();

    public PolarisLoadBalancer(final SDKContext context, final Helper helper) {
        this.routerAPI = RouterAPIFactory.createRouterAPIByContext(context);
        this.helper = Preconditions.checkNotNull(helper);
        this.currentPicker = new PolarisPicker.EmptyPicker(Status.OK);
    }

    @Override
    public void handleResolvedAddresses(ResolvedAddresses resolvedAddresses) {
        List<EquivalentAddressGroup> servers = resolvedAddresses.getAddresses();
        if (servers.isEmpty()) {
            handleNameResolutionError(Status.NOT_FOUND);
            return;
        }

        for (EquivalentAddressGroup addressGroup : servers) {
            final Subchannel subChannel = helper.createSubchannel(CreateSubchannelArgs.newBuilder()
                    .setAddresses(addressGroup)
                    .build());
            subChannels.put(addressGroup, subChannel);
        }
    }

    @Override
    public void handleNameResolutionError(Status error) {
        if (currentState != READY)  {
            updateBalancingState(TRANSIENT_FAILURE, new PolarisPicker.EmptyPicker(error));
        }
    }

    private void updateBalancingState(ConnectivityState state, PolarisPicker picker) {
        if (state != currentState) {
            helper.updateBalancingState(state, picker);
            currentState = state;
            currentPicker = picker;
        }
    }

    @Override
    public void shutdown() {

    }


}
