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
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.StatusDimension;
import com.tencent.polaris.api.utils.StringUtils;
import io.grpc.Attributes;
import io.grpc.Channel;
import io.grpc.ChannelLogger;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer.Subchannel;
import io.grpc.LoadBalancer.SubchannelStateListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisSubChannel extends Subchannel implements Instance {

    private final Subchannel channel;

    private final Instance instance;

    PolarisSubChannel(Instance instance) {
        Preconditions.checkNotNull(instance, "instance");
        this.channel = null;
        this.instance = instance;
    }

    public PolarisSubChannel(Subchannel channel, Instance instance) {
        Preconditions.checkNotNull(channel, "channel");
        Preconditions.checkNotNull(instance, "instance");
        this.channel = channel;
        this.instance = instance;
    }

    public Subchannel getChannel() {
        return channel;
    }

    public Instance getInstance() {
        return instance;
    }

    @Override
    public String getNamespace() {
        return instance.getNamespace();
    }

    @Override
    public String getService() {
        return instance.getService();
    }

    @Override
    public String getRevision() {
        return instance.getRevision();
    }

    @Override
    public CircuitBreakerStatus getCircuitBreakerStatus() {
        return instance.getCircuitBreakerStatus();
    }

    @Override
    public Collection<StatusDimension> getStatusDimensions() {
        return instance.getStatusDimensions();
    }

    @Override
    public CircuitBreakerStatus getCircuitBreakerStatus(StatusDimension statusDimension) {
        return instance.getCircuitBreakerStatus(statusDimension);
    }

    @Override
    public boolean isHealthy() {
        return instance.isHealthy();
    }

    @Override
    public boolean isIsolated() {
        return instance.isIsolated();
    }

    @Override
    public String getProtocol() {
        return instance.getProtocol();
    }

    @Override
    public String getId() {
        return instance.getId();
    }

    @Override
    public String getHost() {
        return instance.getHost();
    }

    @Override
    public int getPort() {
        return instance.getPort();
    }

    @Override
    public String getVersion() {
        return instance.getVersion();
    }

    @Override
    public Map<String, String> getMetadata() {
        return instance.getMetadata();
    }

    @Override
    public boolean isEnableHealthCheck() {
        return instance.isEnableHealthCheck();
    }

    @Override
    public String getRegion() {
        return instance.getRegion();
    }

    @Override
    public String getZone() {
        return instance.getZone();
    }

    @Override
    public String getCampus() {
        return instance.getCampus();
    }

    @Override
    public int getPriority() {
        return instance.getPriority();
    }

    @Override
    public int getWeight() {
        return instance.getWeight();
    }

    @Override
    public String getLogicSet() {
        return instance.getLogicSet();
    }

    @Override
    public void shutdown() {
        channel.shutdown();
    }

    @Override
    public void start(SubchannelStateListener listener) {
        channel.start(listener);
    }

    @Override
    public List<EquivalentAddressGroup> getAllAddresses() {
        return channel.getAllAddresses();
    }

    @Override
    public Channel asChannel() {
        return channel.asChannel();
    }

    @Override
    public ChannelLogger getChannelLogger() {
        return channel.getChannelLogger();
    }

    @Override
    public void updateAddresses(List<EquivalentAddressGroup> addrs) {
        channel.updateAddresses(addrs);
    }

    @Override
    public Object getInternalSubchannel() {
        return channel.getInternalSubchannel();
    }

    @Override
    public void requestConnection() {
        channel.requestConnection();
    }

    @Override
    public Attributes getAttributes() {
        return channel.getAttributes();
    }

    @Override
    public int compareTo(Instance o) {
        return instance.compareTo(o);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PolarisSubChannel) {
            PolarisSubChannel other = (PolarisSubChannel) o;
            return StringUtils.equals(this.getId(), other.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getInstance().getId().hashCode();
    }
}
