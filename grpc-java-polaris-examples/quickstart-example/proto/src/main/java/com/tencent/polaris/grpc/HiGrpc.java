/*
 *
 *  * Tencent is pleased to support the open source community by making Polaris available.
 *  *
 *  * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *  *
 *  * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 *  * compliance with the License. You may obtain a copy of the License at
 *  *
 *  * https://opensource.org/licenses/BSD-3-Clause
 *  *
 *  * Unless required by applicable law or agreed to in writing, software distributed under the License
 *  * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  * or implied. See the License for the specific language governing permissions and limitations under
 *  * the License.
 *
 */

package com.tencent.polaris.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.42.1)",
    comments = "Source: HelloPolaris.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class HiGrpc {

  private HiGrpc() {}

  public static final String SERVICE_NAME = "Hi";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.tencent.polaris.grpc.HelloPolaris.request,
      com.tencent.polaris.grpc.HelloPolaris.response> getSayHiMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "sayHi",
      requestType = com.tencent.polaris.grpc.HelloPolaris.request.class,
      responseType = com.tencent.polaris.grpc.HelloPolaris.response.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.tencent.polaris.grpc.HelloPolaris.request,
      com.tencent.polaris.grpc.HelloPolaris.response> getSayHiMethod() {
    io.grpc.MethodDescriptor<com.tencent.polaris.grpc.HelloPolaris.request, com.tencent.polaris.grpc.HelloPolaris.response> getSayHiMethod;
    if ((getSayHiMethod = HiGrpc.getSayHiMethod) == null) {
      synchronized (HiGrpc.class) {
        if ((getSayHiMethod = HiGrpc.getSayHiMethod) == null) {
          HiGrpc.getSayHiMethod = getSayHiMethod =
              io.grpc.MethodDescriptor.<com.tencent.polaris.grpc.HelloPolaris.request, com.tencent.polaris.grpc.HelloPolaris.response>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "sayHi"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.grpc.HelloPolaris.request.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.grpc.HelloPolaris.response.getDefaultInstance()))
              .setSchemaDescriptor(new HiMethodDescriptorSupplier("sayHi"))
              .build();
        }
      }
    }
    return getSayHiMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static HiStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HiStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HiStub>() {
        @java.lang.Override
        public HiStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HiStub(channel, callOptions);
        }
      };
    return HiStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static HiBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HiBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HiBlockingStub>() {
        @java.lang.Override
        public HiBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HiBlockingStub(channel, callOptions);
        }
      };
    return HiBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static HiFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HiFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HiFutureStub>() {
        @java.lang.Override
        public HiFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HiFutureStub(channel, callOptions);
        }
      };
    return HiFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class HiImplBase implements io.grpc.BindableService {

    /**
     */
    public void sayHi(com.tencent.polaris.grpc.HelloPolaris.request request,
        io.grpc.stub.StreamObserver<com.tencent.polaris.grpc.HelloPolaris.response> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSayHiMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSayHiMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.tencent.polaris.grpc.HelloPolaris.request,
                com.tencent.polaris.grpc.HelloPolaris.response>(
                  this, METHODID_SAY_HI)))
          .build();
    }
  }

  /**
   */
  public static final class HiStub extends io.grpc.stub.AbstractAsyncStub<HiStub> {
    private HiStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HiStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HiStub(channel, callOptions);
    }

    /**
     */
    public void sayHi(com.tencent.polaris.grpc.HelloPolaris.request request,
        io.grpc.stub.StreamObserver<com.tencent.polaris.grpc.HelloPolaris.response> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSayHiMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class HiBlockingStub extends io.grpc.stub.AbstractBlockingStub<HiBlockingStub> {
    private HiBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HiBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HiBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.tencent.polaris.grpc.HelloPolaris.response sayHi(com.tencent.polaris.grpc.HelloPolaris.request request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSayHiMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class HiFutureStub extends io.grpc.stub.AbstractFutureStub<HiFutureStub> {
    private HiFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HiFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HiFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.tencent.polaris.grpc.HelloPolaris.response> sayHi(
        com.tencent.polaris.grpc.HelloPolaris.request request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSayHiMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SAY_HI = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final HiImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(HiImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SAY_HI:
          serviceImpl.sayHi((com.tencent.polaris.grpc.HelloPolaris.request) request,
              (io.grpc.stub.StreamObserver<com.tencent.polaris.grpc.HelloPolaris.response>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class HiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    HiBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.tencent.polaris.grpc.HelloPolaris.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Hi");
    }
  }

  private static final class HiFileDescriptorSupplier
      extends HiBaseDescriptorSupplier {
    HiFileDescriptorSupplier() {}
  }

  private static final class HiMethodDescriptorSupplier
      extends HiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    HiMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (HiGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new HiFileDescriptorSupplier())
              .addMethod(getSayHiMethod())
              .build();
        }
      }
    }
    return result;
  }
}
