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

import static io.grpc.ConnectivityState.CONNECTING;
import static io.grpc.ConnectivityState.IDLE;
import static io.grpc.ConnectivityState.READY;
import static io.grpc.ConnectivityState.TRANSIENT_FAILURE;

import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.RouterAPIFactory;
import com.tencent.polaris.grpc.loadbalance.PolarisPicker.EmptyPicker;
import com.tencent.polaris.router.api.core.RouterAPI;
import io.grpc.ConnectivityState;
import io.grpc.ConnectivityStateInfo;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.Status;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import shade.polaris.com.google.common.base.Preconditions;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisLoadBalancer extends LoadBalancer {

    private final RouterAPI routerAPI;

    private final Helper helper;

    private final String rule;

    private ConnectivityState currentState;

    private static final Status EMPTY_OK = Status.OK.withDescription("no subChannels ready");

    private final Map<EquivalentAddressGroup, Subchannel> subChannels = new ConcurrentHashMap<>();

    public PolarisLoadBalancer(final SDKContext context, final String rule, final Helper helper) {
        this.routerAPI = RouterAPIFactory.createRouterAPIByContext(context);
        this.rule = rule;
        this.helper = Preconditions.checkNotNull(helper);
    }

    @Override
    public void handleResolvedAddresses(ResolvedAddresses resolvedAddresses) {
        List<EquivalentAddressGroup> servers = resolvedAddresses.getAddresses();
        if (servers.isEmpty()) {
            handleNameResolutionError(Status.NOT_FOUND);
            return;
        }

        Set<EquivalentAddressGroup> removed = Utils.setsDifference(subChannels.keySet(), new HashSet<>(servers));
        for (EquivalentAddressGroup addressGroup : servers) {
            final Subchannel subChannel = helper.createSubchannel(CreateSubchannelArgs.newBuilder()
                    .setAddresses(addressGroup)
                    .build());

            subChannel.start(state -> processSubChannelState(subChannel, state));
            subChannels.put(addressGroup, subChannel);
            subChannel.requestConnection();
            subChannels.put(addressGroup, subChannel);
        }

        removed.forEach(entry -> {
            Subchannel channel = subChannels.remove(entry);
            Utils.shutdownSubChannel(channel);
        });

    }

    @Override
    public void handleNameResolutionError(Status error) {
        if (currentState != READY) {
            updateBalancingState(TRANSIENT_FAILURE, new EmptyPicker(error));
        }
    }

    private void processSubChannelState(Subchannel subChannel, ConnectivityStateInfo stateInfo) {
        if (subChannels.get(subChannel.getAddresses()) != subChannel) {
            return;
        }
        if (stateInfo.getState() == TRANSIENT_FAILURE || stateInfo.getState() == IDLE) {
            helper.refreshNameResolution();
        }
        if (stateInfo.getState() == IDLE) {
            subChannel.requestConnection();
        }
        Utils.Ref<ConnectivityStateInfo> subChannelStateRef = Utils.getSubChannelStateInfoRef(subChannel);
        if (subChannelStateRef.value.getState().equals(TRANSIENT_FAILURE)) {
            if (stateInfo.getState().equals(CONNECTING) || stateInfo.getState().equals(IDLE)) {
                return;
            }
        }
        subChannelStateRef.value = stateInfo;
        updateBalancingState();
    }

    private void updateBalancingState() {
        List<Subchannel> activeList = Utils.filterNonFailingSubChannels(subChannels.values());
        if (activeList.isEmpty()) {
            boolean isConnecting = false;
            Status aggStatus = EMPTY_OK;
            for (Subchannel subchannel : subChannels.values()) {
                ConnectivityStateInfo stateInfo = Utils.getSubChannelStateInfoRef(subchannel).value;
                if (stateInfo.getState() == CONNECTING || stateInfo.getState() == IDLE) {
                    isConnecting = true;
                }
                if (aggStatus == EMPTY_OK || !aggStatus.isOk()) {
                    aggStatus = stateInfo.getStatus();
                }
            }
            updateBalancingState(isConnecting ? CONNECTING : TRANSIENT_FAILURE, new EmptyPicker(aggStatus));
        } else {
            updateBalancingState(READY, new PolarisPicker(activeList, rule, this.routerAPI));
        }
    }

    private void updateBalancingState(ConnectivityState state, SubchannelPicker picker) {
        if (state != currentState) {
            helper.updateBalancingState(state, picker);
            currentState = state;
        }
    }

    @Override
    public void shutdown() {

    }


}
