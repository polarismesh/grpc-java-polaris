# grpc-java-polaris

English | [简体中文](./README-zh.md)

## Introduction

grpc-java-polaris is a service management component based on the Grpc extension, which is convenient for developers who use Grpc to quickly access polaris and use the functions of service registration, load balancing, fusing, and current limiting provided by polaris.

## Catalog Introduction

- grpc-java-polaris　　　　　　　　Core code based on Grpc extended functions
- grpc-java-polaris-example　　　　Get started quickly with the use case of grpc-java-polaris

## How To Use

### Source installation

Copy the following command to clone the source code to the local:

```
git clone https://github.com/polarismesh/grpc-java-polaris.git
```

Then execute the install command to install the grpc-java-polaris project to the local maven warehouse:

```
mvn clean install -U -Dmaven.test.skip=true
```

### Use

Add grpc-java-polaris dependency to the developed Grpc project:
```
<dependency>
   <groupId>com.tencent.polaris</groupId>
   <artifactId>grpc-java-polaris</artifactId>
   <version>${grpc-java-polaris.version}</version>
</dependency>
```

The server uses the `PolarisGrpcServer` provided by the grpc-java-polaris package to create a grpc server. `PolarisGrpcServer` internally encapsulates service registration, heartbeat logic, etc:
```
PolarisGrpcServer polarisGrpcServer = PolarisGrpcServer.builder()
        .port(50051)
        .namespace("default")
        .serviceName("grpc-demo-java")
        .metaData(null)
        .bindableServices(services)
        .build();
        
polarisGrpcServer.start();
```

The client needs to register the PolarisNameResolverProvider in the NameResolverRegistry to provide the service discovery function:
```
NameResolverRegistry.getDefaultRegistry().register(new PolarisNameResolverProvider());
ManagedChannel channel = ManagedChannelBuilder.forTarget("polaris://grpc-demo-java:8080?namespace=default")
        .usePlaintext().build();
```

### Examples

- [QuickStart](./grpc-java-polaris-example)
