# gRPC-Java-Polaris

[English](./README.md) | 简体中文

## 介绍

gRPC-Java-Polaris 是基于 Grpc 扩展的服务治理组件，方便使用 Grpc 的开发者快速接入 polaris，并使用 polaris 提供的服务注册、负载均衡、熔断、限流等功能。

## 目录介绍

- grpc-java-polaris              基于 Grpc 扩展功能的核心代码
- grpc-java-polaris-example      快速上手 grpc-java-polaris 的使用用例

## 快速上手

### 源码安装

复制以下命令将源码 clone 到本地： 

```
git clone https://github.com/polarismesh/grpc-java-polaris.git
```

然后执行 install 命令将 grpc-java-polaris 项目安装到本地的 maven 仓库：

```
mvn clean install -U -Dmaven.test.skip=true
```

### 如何使用

在开发的 Grpc 项目中添加 grpc-java-polaris 的依赖：
```
<dependency>
   <groupId>com.tencent.polaris</groupId>
   <artifactId>grpc-java-polaris</artifactId>
   <version>${grpc-java-polaris.version}</version>
</dependency>
```

服务端使用 grpc-java-polaris 包提供的 `PolarisGrpcServer` 来创建 grpc server，`PolarisGrpcServer` 内部封装了服务注册、心跳逻辑等：
```
PolarisGrpcServer polarisGrpcServer = PolarisGrpcServer.builder()
        .port(50051)
        .namespace("default")
        .applicationName("grpc-demo-java")
        .metadata(null)
        .bindableServices(services)
        .build();
        
polarisGrpcServer.start();
```

客户端使用时需要将 `ManagedChannelBuilder` 替换为 `PolarisManagedChannelBuilder`：
```
ManagedChannel channel = PolarisManagedChannelBuilder.forTarget("polaris://grpc-demo-java:8080?namespace=default")
        .usePlaintext().build();
```

### 示例

- [快速入门](./grpc-java-polaris-examples/quickstart-example)
