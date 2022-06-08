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

import com.google.common.base.Preconditions;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.api.RouterAPIFactory;
import com.tencent.polaris.grpc.loadbalance.PolarisPicker.EmptyPicker;
import com.tencent.polaris.grpc.util.Common;
import com.tencent.polaris.grpc.util.GrpcHelper;
import com.tencent.polaris.router.api.core.RouterAPI;
import io.grpc.Attributes;
import io.grpc.ConnectivityState;
import io.grpc.ConnectivityStateInfo;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.Status;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisLoadBalancer extends LoadBalancer {

    private static final Status EMPTY_OK = Status.OK.withDescription("no subChannels ready");

    private final ConsumerAPI consumerAPI;

    private final RouterAPI routerAPI;

    private final Helper helper;

    private final AtomicReference<ConnectivityState> currentState = new AtomicReference<>(IDLE);

    private final Map<EquivalentAddressGroup, PolarisSubChannel> subChannels = new ConcurrentHashMap<>();

    private final Function<EquivalentAddressGroup, PolarisSubChannel> function = new Function<EquivalentAddressGroup, PolarisSubChannel>() {
        @Override
        public PolarisSubChannel apply(EquivalentAddressGroup addressGroup) {

            Attributes newAttributes = addressGroup.getAttributes().toBuilder()
                    .set(GrpcHelper.STATE_INFO, new GrpcHelper.Ref<>(ConnectivityStateInfo.forNonError(IDLE)))
                    .build();

            final Subchannel subChannel = helper.createSubchannel(CreateSubchannelArgs.newBuilder()
                    .setAddresses(addressGroup)
                    .setAttributes(newAttributes)
                    .build());

            subChannel.start(state -> processSubChannelState(subChannel, state));
            subChannel.requestConnection();

            return new PolarisSubChannel(subChannel, newAttributes.get(Common.INSTANCE_KEY));
        }
    };

    private final Predicate<ConnectivityState> predicate = (state) -> {
        if (state == READY) {
            return true;
        }

        return state != currentState.get();
    };

    private ServiceInfo sourceService;

    public PolarisLoadBalancer(final SDKContext context, final Helper helper) {
        this.consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(context);
        this.routerAPI = RouterAPIFactory.createRouterAPIByContext(context);
        this.helper = Preconditions.checkNotNull(helper);
    }

    @Override
    public void handleResolvedAddresses(ResolvedAddresses resolvedAddresses) {
        if (sourceService == null) {
            this.sourceService = resolvedAddresses.getAttributes().get(Common.SOURCE_SERVICE_INFO);
        }

        List<EquivalentAddressGroup> servers = resolvedAddresses.getAddresses();
        if (servers.isEmpty()) {
            handleNameResolutionError(Status.NOT_FOUND);
            return;
        }

        Set<EquivalentAddressGroup> removed = GrpcHelper.setsDifference(subChannels.keySet(), new HashSet<>(servers));
        for (EquivalentAddressGroup addressGroup : servers) {
            subChannels.computeIfAbsent(addressGroup, function);
        }

        removed.forEach(entry -> {
            Subchannel channel = subChannels.remove(entry);
            GrpcHelper.shutdownSubChannel(channel);
        });

    }

    @Override
    public void handleNameResolutionError(Status error) {
        if (currentState.get() != READY) {
            updateBalancingState(TRANSIENT_FAILURE, new EmptyPicker(error));
        }
    }

    private void processSubChannelState(Subchannel subChannel, ConnectivityStateInfo stateInfo) {
        PolarisSubChannel channel = subChannels.get(subChannel.getAddresses());
        if (Objects.isNull(channel) || channel.getChannel() != subChannel) {
            return;
        }
        if (stateInfo.getState() == TRANSIENT_FAILURE || stateInfo.getState() == IDLE) {
            helper.refreshNameResolution();
        }
        if (stateInfo.getState() == IDLE) {
            subChannel.requestConnection();
        }
        GrpcHelper.Ref<ConnectivityStateInfo> subChannelStateRef = GrpcHelper.getSubChannelStateInfoRef(subChannel);
        if (subChannelStateRef.getValue().getState().equals(TRANSIENT_FAILURE)) {
            if (stateInfo.getState().equals(CONNECTING) || stateInfo.getState().equals(IDLE)) {
                return;
            }
        }
        subChannelStateRef.setValue(stateInfo);
        updateBalancingState();
    }

    private void updateBalancingState() {
        AtomicReference<Attributes> holder = new AtomicReference<>();
        Map<PolarisSubChannel, PolarisSubChannel> activeList = GrpcHelper.filterNonFailingSubChannels(subChannels,
                holder);
        if (activeList.isEmpty()) {
            boolean isConnecting = false;
            Status aggStatus = EMPTY_OK;
            for (Subchannel subchannel : subChannels.values()) {
                ConnectivityStateInfo stateInfo = GrpcHelper.getSubChannelStateInfoRef(subchannel).getValue();
                if (stateInfo.getState() == CONNECTING || stateInfo.getState() == IDLE) {
                    isConnecting = true;
                }
                if (aggStatus == EMPTY_OK || !aggStatus.isOk()) {
                    aggStatus = stateInfo.getStatus();
                }
            }
            updateBalancingState(isConnecting ? CONNECTING : TRANSIENT_FAILURE, new EmptyPicker(aggStatus));
        } else {
            updateBalancingState(READY, new PolarisPicker(activeList, this.consumerAPI, this.routerAPI, sourceService, holder.get()));
        }
    }

    private void updateBalancingState(ConnectivityState state, SubchannelPicker picker) {
        if (predicate.test(state)) {
            helper.updateBalancingState(state, picker);
            currentState.set(state);
        }
    }

    @Override
    public void shutdown() {

    }

}
