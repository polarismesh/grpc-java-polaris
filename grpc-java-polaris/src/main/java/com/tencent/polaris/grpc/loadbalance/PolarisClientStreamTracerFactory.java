package com.tencent.polaris.grpc.loadbalance;

import com.tencent.polaris.grpc.util.ClientCallInfo;
import io.grpc.ClientStreamTracer;
import io.grpc.ClientStreamTracer.Factory;
import io.grpc.ClientStreamTracer.StreamInfo;
import io.grpc.Metadata;

/**
 * Factory class for {@link ClientStreamTracer}.
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class PolarisClientStreamTracerFactory extends Factory {

    private final ClientCallInfo callInfo;

    public PolarisClientStreamTracerFactory(final ClientCallInfo callInfo) {
        super();
        this.callInfo = callInfo;
    }

    @Override
    public ClientStreamTracer newClientStreamTracer(StreamInfo info, Metadata headers) {
        return new PolarisClientStreamTracer(info, headers, callInfo);
    }
}
