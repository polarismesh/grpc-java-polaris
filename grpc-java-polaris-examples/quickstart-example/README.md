# grpc-java-polaris Quick start

English | [简体中文](README-zh.md)

Provide examples of applications developed based on the gRPC framework (including consumer and provider applications), 
and demonstrate how applications developed using the gRPC framework can quickly connect to Polaris.

## Content

- provider: gRPC server application, demo service register, deregister, heartbeat.
- consumer: gRPC client application, demo service discovery, and load balance.  

## Instruction

### Configuration

Modify polaris.yaml in provider and consumer, which is showed as below: besides, ${ip} and ${port} is the address of polaris server.

```yaml
global:
  serverConnector:
    addresses:
    - ${ip}:${port}
```

## Start application

### Start Provider

Maven compilation and packaging:
```shell
cd provider
mvn clean package -U -Dmaven.test.skip=true
```

Then run the compiled jar package:

```shell
java -jar quickstart-provider-1.0.0-SNAPSHOT.jar
```

### Start Consumer

Maven compilation and packaging:
```shell
cd consumer
mvn clean package -U -Dmaven.test.skip=true
```

Then run the compiled jar package:

```shell
 java -jar quickstart-consumer-1.0.0-SNAPSHOT.jar
```

### Verify

#### Check polaris console

Login into polaris console, and check the instances in Service grpc-demo-java

#### Invoke by http call

Invoke http call，replace ${app.port} to the consumer port (40041 by default).
```shell
curl -X GET 'http://localhost:40041/polaris/grpc/quickstart/consumer?value="hello-polaris'
```

expect：`hello-polaris`