A Managerment API Gateway in Java . Fizz Gateway 是一个基于 Java开发的微服务网关，能够实现热服务编排、自动授权选择、线上服务脚本编码、在线测试、高性能路由、API审核管理等目的，拥有强大的自定义插件系统可以自行扩展，并且提供友好的图形化配置界面，能够快速帮助企业进行API服务治理、减少中间层胶水代码以及降低编码投入、提高 API 服务的稳定性和安全性。

### Fizz的设计

<img width="600" src="https://github.com/wehotel/fizz-gateway-community/blob/master/docs/fizz_design.png" />

### 产品特性

- 集群管理：Fizz网关节点是无状态的，配置信息自动同步，支持节点水平拓展和多集群部署。
- 服务编排：支持热服务编排能力，支持前后端编码，随时随地更新API。
- 负载均衡：支持round-robin负载均衡。
- 服务发现：支持从Eureka注册中心发现后端服务器。
- 配置中心：支持接入apollo配置中心。
- HTTP反向代理：隐藏真实后端服务，支持 Rest API反向代理。
- 访问策略：支持不同策略访问不同的API、配置不同的鉴权等。
- IP黑白名单：支持配置IP黑白名单。
- 自定义插件：强大的插件机制支持自由扩展。
- 可扩展：简单易用的插件机制方便扩展功能。
- 高性能：性能在众多网关之中表现优异。
- 版本控制：支持操作的发布和多次回滚。
- 管理后台：通过管理后台界面对网关集群进行各项配置。

### 基准测试

我们将Fizz与Spring官方spring-cloud-gateway进行比较，使用相同的环境和条件，测试对象均为单个节点。

- Intel(R) Xeon(R) CPU X5675 @ 3.07GHz * 4
- Linux version 3.10.0-327.el7.x86_64
- 8G RAM


|         产品         | QPS     | 90% Latency(ms) |
| :------------------: | ------- | -------------------- |
|    直接访问后端服务    | 9087.46 | 10.76 |
|     fizz-gateway     | 5927.13 | 19.86 |
| spring-cloud-gateway | 5044.04 | 22.91 |




### 部署说明

[部署教程](https://wehotel.github.io/fizz-gateway-community/guide/installation/) 




### 授权说明

1. 网关核心项目fizz-gateway-community社区版本以GNU v3的方式进行的开放，可以免费使用。

2. 管理后台项目(fizz-manager-professional和fizz-admin-professional)作为商业版本仅开放二进制包的免费下载，而商业项目请联系我们（524423586@qq.com）进行授权。


