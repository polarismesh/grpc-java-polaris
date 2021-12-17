# grpc-java-polaris 快速入门

[English](./README.md) | 简体中文

提供基于gRPC框架开发的应用示例（包含consumer和provider两个应用），演示使用 gRPC 框架开发的应用如何快速接入北极星。

## 目录介绍

- provider: gRPC 服务端示例，演示服务实例注册、反注册、以及心跳上报保活的功能。
- consumer: gRPC 客户端示例，演示服务发现、负载均衡、以及服务调用的功能。

## 使用说明

### 修改配置

在 ```provider``` 以及 ```consumer``` 两个项目中，修改```polaris.yaml```，修改后配置如下所示。
其中，```${ip}```和```${port}```为Polaris后端服务的IP地址与端口号。

```yaml
global:
  serverConnector:
    addresses:
    - ${ip}:${port}
```

## 启动样例

### 启动Provider

maven 编译打包：
```shell
cd provider
mvn clean package -U -Dmaven.test.skip=true
```

然后运行编译好的jar包：

```shell
java -jar quickstart-provider-1.0.0-SNAPSHOT.jar
```

### 启动Consumer

maven 编译打包：
```shell
cd consumer
mvn clean package -U -Dmaven.test.skip=true
```

然后运行编译好的jar包：

```shell
 java -jar quickstart-consumer-1.0.0-SNAPSHOT.jar
```

### 验证

#### 控制台验证

登录polaris控制台，可以看到 grpc-demo-java 服务下存在对应的provider实例。

#### HTTP调用

执行http调用，其中`${app.port}`替换为consumer的监听端口（默认为40041）。
```shell
curl -X GET 'http://localhost:40041/polaris/grpc/quickstart/consumer?value="hello-polaris'
```

预期返回值：`hello-polaris`