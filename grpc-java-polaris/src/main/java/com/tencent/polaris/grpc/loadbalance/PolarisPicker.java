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
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.grpc.util.ClientCallInfo;
import com.tencent.polaris.grpc.util.Common;
import com.tencent.polaris.grpc.util.GrpcHelper;
import io.grpc.Attributes;
import io.grpc.LoadBalancer.Helper;
import io.grpc.LoadBalancer.PickResult;
import io.grpc.LoadBalancer.PickSubchannelArgs;
import io.grpc.LoadBalancer.Subchannel;
import io.grpc.LoadBalancer.SubchannelPicker;
import io.grpc.Status;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main balancing logic.  It <strong>must be thread-safe</strong>. Typically it should only
 * synchronize on its own state, and avoid synchronizing with the LoadBalancer's state.
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisPicker extends SubchannelPicker {

    private static final Logger LOG = LoggerFactory.getLogger(PolarisPicker.class);

    private final Map<PolarisSubChannel, PolarisSubChannel> channels;

    private final ConsumerAPI consumerAPI;

    private final Attributes attributes;

    private final ServiceInfo sourceService;

    public PolarisPicker(final Map<PolarisSubChannel, PolarisSubChannel> channels,
            final ConsumerAPI consumerAPI, final ServiceInfo sourceService, final Attributes attributes) {
        this.channels = channels;
        this.consumerAPI = consumerAPI;
        this.attributes = attributes;
        this.sourceService = sourceService;
    }

    @Override
    public PickResult pickSubchannel(PickSubchannelArgs args) {
        if (channels.isEmpty()) {
            return PickResult.withNoResult();
        }

        final String targetNamespace = attributes.get(Common.TARGET_NAMESPACE_KEY);
        final String targetService = attributes.get(Common.TARGET_SERVICE_KEY);

        final GetOneInstanceRequest request = new GetOneInstanceRequest();
        request.setNamespace(targetNamespace);
        request.setService(targetService);

        final ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setNamespace("default");
        serviceInfo.setMetadata(GrpcHelper.collectLabels(args.getHeaders(), (val) -> true));

        if (Objects.nonNull(sourceService)) {
            request.setMetadata(sourceService.getMetadata());
            serviceInfo.setNamespace(sourceService.getNamespace());
            serviceInfo.setService(sourceService.getService());
        }
        request.setServiceInfo(serviceInfo);

        try {
            InstancesResponse response = consumerAPI.getOneInstance(request);
            Instance instance = response.getInstances()[0];
            Subchannel channel = channels.get(new PolarisSubChannel(instance));

            return channel == null ? PickResult.withError(Status.NOT_FOUND) : PickResult.withSubchannel(channel,
                    new PolarisClientStreamTracerFactory(ClientCallInfo.builder()
                            .consumerAPI(consumerAPI)
                            .instance(instance)
                            .targetNamespace(targetNamespace)
                            .targetService(targetService)
                            .method(args.getMethodDescriptor().getBareMethodName())
                            .build()));
        } catch (PolarisException e) {
            LOG.error("pick subChannel fail", e);
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
