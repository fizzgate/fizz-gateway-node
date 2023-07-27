English | [简体中文](./README.md)

<p align="center" >
    <a href="https://www.fizzgate.com"><img src="https://www.fizzgate.com/fizz/nav-bar/logo.png?v=1" width="70%"></a>
</p>
<p>
  <img alt="Version" src="https://img.shields.io/badge/version-3.0.2-beta1-blue.svg?cacheSeconds=2592000" />
  <a href="http://www.fizzgate.com/fizz-gateway-node/" target="_blank">
    <img alt="Documentation" src="https://img.shields.io/badge/documentation-yes-brightgreen.svg" />
  </a>
  <a href="#" target="_blank">
    <img alt="License: AGPL--3.0" src="https://img.shields.io/badge/License-AGPL--3.0-yellow.svg" />
  </a>
  <a href="https://github.com/fizzgate/fizz-gateway-node/actions" target="_blank">
    <img alt="Java CI with Maven" src="https://github.com/fizzgate/fizz-gateway-node/workflows/Java%20CI%20with%20Maven/badge.svg?branch=master" />
  </a>
</p>

- **latest QQ group**: 512164278

## What 's FizzGate？

An Aggregation API Gateway in Java . FizzGate is a Java-based microservice gateway that can realize hot service aggregation, automatic authorization selection, online service script coding, online testing, high-performance routing, API audit management and other purposes. It has a powerful The custom plug-in system can be extended by youself, and provides a friendly graphical configuration interface, which can quickly help enterprises to manage API services, reduce middle layer glue code, reduce coding investment, and improve the stability and security of API services.


## Demo

http://demo.fizzgate.com/

account/password:`admin`/`Aa123!`

health checking url：http://demo.fizzgate.com/admin/health

API access：http://demo.fizzgate.com/proxy/[Service Name]/[API Path]

## FizzGate's Design

<img width="500" src="https://user-images.githubusercontent.com/184315/97130741-33a90d80-177d-11eb-8680-f589a36e44b3.png" />

## FizzGate's typical scene

<img width="90%" src="https://user-images.githubusercontent.com/6129661/216249866-71eb54de-d2e8-44ce-8e70-a1ca1f51553d.png" />

## Product Features

- Cluster management: FizzGate nodes are stateless with configuration information that is automatically synchronized, and horizontal expansion of nodes and multi-cluster deployment are supported.
- Service aggregation: supports hot http/dubbo/grpc service aggregation capabilities, support front-end and back-end coding, and update API anytime and anywhere;
- Load balancing: supports round-robin load balancing;
- Multiple Registration Center: supports discovery of back-end servers from the Eureka or Nacos registry;
- Configuration center: supports access to apollo configuration center;
- HTTP reverse proxy: hide the real back-end services and support Rest API reverse proxy;
- Access strategy: support different strategies to access different APIs, configure different authentication, etc;
- IP black and white list: support the configuration of IP black and white list;
- Custom plug-in: powerful plug-in mechanism supports free expansion;
- Extensible: the easy-to-use plug-in mechanism facilitates the expansion of functions;
- High performance: the performance is excellent among many gateways;
- Version control: support release and multiple rollbacks of operations;
- Management backend: configure the gateway cluster through the management backend interface;
- Callback Managerment：supports managerment、subscription、replay and logging of callback;
- Multistage traffic limit：supports multisate traffic limit of fine-grain parallelism, according to service、interface、APP_ID or ip; 
- Microservice document:open microservice document management of enterprise level, and more convenient system integration;
- Public network dedicated line：supports establishing a fully protected private connection channel in the public network;
- Policy fusing: multiple recovery policy fusing configurations are made according to the service or specific address.

## Benchmarks

We compare FzzGate with the major gateway products on the market, using the same environment and conditions, and the test objects are under single node. The Mock interface simulates a 20ms latency with a packet size of about 2K.

- Intel(R) Xeon(R) CPU E5-2650 v3 @ 2.30GHz * 4
- Linux version 3.10.0-957.21.3.el7.x86_64
- 8G RAM

|  Category  |  Product name  | QPS of <br/>600 connections | 90% Latency(ms) of <br/>600 connections | QPS of <br/>1000 connections | 90% Latency(ms) of <br/>1000 connections |
| :------------------ | :------------------ | :-------: | :-------: | :-------: | :-------: |
| Backend Service |    direct access    | 23540| 32.19 | 27325| 52.09 |
| Traffic Gateway | kong <br/>v2.4.1 | 15662 | 50.87 | 17152 | 84.3 |
| Application Gateway | fizz-gateway-node <br/>v2.0.0 | 12206 | 65.76 | 12766 | 100.34 |
| Application Gateway | spring-cloud-gateway <br/>v2.2.9 | 11323 | 68.57 | 10472 | 127.59 |
| Application Gateway | shenyu <br/>v2.3.0 | 9284 | 92.98 | 9939 | 148.61 |


## Version comparison

- fizz-gateway-node： Community Edition

- fizz-manager-professional：Management backend professional version (backend)

- fizz-admin-professional：Management backend professional version (frontend)

| fizz-gateway-node | fizz-manager-professional | fizz-admin-professional |
| ---------------------- | ------------------------- | ----------------------- |
| v1.0.0                 | v1.0.0                    | v1.0.0                  |
| v1.1.0                 | v1.1.0                    | v1.1.0                  |
| v1.1.1                 | v1.1.1                    | v1.1.1                  |
| v1.2.0                 | v1.2.0                    | v1.2.0                  |

Starting from v1.3.0, the frontend and backend of the management backend are merged into one package

- fizz-gateway-node： Community Edition

- fizz-manager-professional：Management backend professional version

| fizz-gateway-node | fizz-manager-professional |
|-------------------|---------------------------|
| v1.3.0            | v1.3.0                    |
| ...               | ...                       |
| v2.6.6            | v2.6.6                    |
| v2.7.0            | v2.7.0                    |
| v2.7.1            | v2.7.1                    |
| v2.7.2            | v2.7.2                    |
| v3.0.0-SNAPSHOT   | v3.0.0-SNAPSHOT           |
| v3.0.2-beta1      | v3.0.2-beta1              |


Please download the corresponding management backend version according to the version of the community version

## Deployment instructions

[Detailed deployment tutorial>>>](http://www.fizzgate.com/guide/installation/) 

### Installation dependencies

Install the following dependent software:

-Redis v2.8 or above
-MySQL v5.7 or above
-Apollo Configuration Center (optional)
-Eureka v1.10.17 or Nacos (v2.0.4 or above) Service Registry (optional)

Dependent installation can refer to detailed deployment tutorial

### Install FizzGate

#### 一、Install management backend

Download the fizz-manager-professional installation package from github's releases (https://wj.qq.com/s2/8682608/8fe2/)

##### Management backend (fizz-manager-professional)

Description:

1. The `{version}` that appears in the following installation steps represents the version number of the management backend used, such as `1.3.0`.

installation method 1: binary package:

1. Unzip the `fizz-manager-professional-{version}.zip` installation package
2. For the first installation, execute the `fizz-manager-professional-{version}-mysql.sql` database script, upgrade from a low version to a high version, and choose to execute the corresponding upgrade script in the update directory
3. Modify the `application-prod.yml` file, and modify the relevant configuration to the configuration of the deployment environment
4. Linux startup Execute the `chmod +x boot.sh` command to increase the execution authority of `boot.sh`; execute the `./boot.sh start` command to start the service, support start/stop/restart/status commands
5. Windows startup Execute `.\boot.cmd start` command to start the service, support start/stop/restart/status command

Installation method 2: docker:

Download SQL script from page: https://github.com/fizzgate/fizz-gateway-node/releases/tag/{version} （replace {version} with corresponding version number）

For the first installation, execute the `fizz-manager-professional-{version}-mysql.sql` database script, upgrade from a low version to a high version, and choose to execute the corresponding upgrade script in the update directory

1. Download docker image：docker pull fizzgate/fizz-manager-professional:{version}
2. Modify Redis & database configuration by env parameters and run with below docker command
```sh
docker run --rm -d -p 8000:8000 \
-e "spring.redis.host={your redis host IP}" \
-e "spring.redis.port={your redis port}" \
-e "spring.redis.password={your redis password}" \
-e "spring.redis.database={your redis database}" \
-e "spring.datasource.url=jdbc:mysql://{your MySQL database host IP}:3306/fizz_manager?useSSL=false&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&serverTimezone=GMT%2B8&nullCatalogMeansCurrent=true&allowPublicKeyRetrieval=true" \
-e "spring.datasource.username={your MySQL database username}" \
-e "spring.datasource.password={your MySQL database password}" \
fizzgate/fizz-manager-professional:{version}
```

or using external configuration file and output log to host server by mount volume, configuration file could be achieved from binary package, create fizz-manager-professional/config and fizz-manager-professional/logs directories in host server, place application-prod.yml configuration files to config folder, run with below docker command in fizz-manager-professional folder:

```sh
cd fizz-manager-professional
docker run --rm -d -p 8000:8000 \
-v $PWD/config:/opt/fizz-manager-professional/config \
-v $PWD/logs:/opt/fizz-manager-professional/logs fizzgate/fizz-manager-professional:{version}
```

After the service is started, visit http://{deployment machine IP address}:8000/#/login, and log in with the super administrator account `admin` password `Aa123!`

#### 二、Install fizz-gateway-node community edition

Description:

1. Support configuration center: apollo, nacos, support registration center: eureka, nacos.refer to application.yml file with more detailed configurations.
2. If you use the apollo configuration center, you can move the content of the application.yml file to the configuration center (the application name on apollo is: fizz-gateway); if you don't use apollo, you can remove the apollo parameter in the startup command below.
3. The `{version}` that appears in the following installation steps represents the version number of the gateway used, such as `1.3.0`.

Installation method 1: binary package:

1. Download the latest binary package of fizz-gateway-node and upzip to a directory, modify the configuration of the configuration center, registry, and redis in the application.yml configuration file (redis configuration needs to be consistent with the management backend).
2. Modify the apollo connection and JVM memory configuration of the boot.sh script
3. Linux startup Execute `./boot.sh start` command to start the service, support start/stop/restart/status command
4. Windows startup Execute `.\boot.cmd start` command to start the service, support start/stop/restart/status command

Installation method 2: source code:

1. The latest code on the local clone warehouse, modify the configuration of the configuration center, registry, and redis in the application.yml configuration file (redis configuration needs to be consistent with the management backend)
2. Execute the Maven command `mvn clean package install -DskipTests=true` package in the project root directory fizz-gateway-node
3. Execute the Maven command `mvn clean package -DskipTests=true` package in the project directory fizz-gateway-node/fizz-bootstrap
4. Enter fizz-gateway-node/fizz-bootstrap/target/fizz-gateway-node directory and Execute `./boot.sh start` command to start the service, support start/stop/restart/status command

Installation method 3: docker:

1. Download docker image：docker pull fizzgate/fizz-gateway-node:{version}
2. Modify Redis configuration by env parameters and run with below docker command
```sh
docker run --rm -d -p 8600:8600 \
-e "aggregate.redis.host={your redis host IP}" \
-e "aggregate.redis.port={your redis port}" \
-e "aggregate.redis.password={your redis password}" \
-e "aggregate.redis.database={your redis database}" \
fizzgate/fizz-gateway-node:{version}
```

or using external configuration file and output log to host server by mount volume, configuration file could be achieved from source code or binary package, create fizz-gateway-node/config and fizz-gateway-node/logs directories in host server, place application.yml and log4j2-spring.xml configuration files to config folder, run with below docker command in fizz-gateway-node folder:

```sh
cd fizz-gateway-node
docker run --rm -d -p 8600:8600 \
-v $PWD/config:/opt/fizz-gateway-node/config \
-v $PWD/logs:/opt/fizz-gateway-node/logs fizzgate/fizz-gateway-node:{version}
```

Finally visit the gateway, the address format is: http://127.0.0.1:8600/[Service name]/[API Path]

## Official technical exchange group

FizzGate官方技术交流①群（已满）

FizzGate官方技术交流②群（已满）

FizzGate官方技术交流③群：512164278

<img width="250" src="https://user-images.githubusercontent.com/184315/97130743-3572d100-177d-11eb-97c8-7599a22c7c04.png" />

## Related acticles

[服务器减少50%，研发效率提高86%，我们的管理型网关FizzGate自研之路](https://www.infoq.cn/article/9wdfiOILJ0CYsVyBQFpl)

[简单易用的微服务聚合网关首选：FizzGate安装教程](https://my.oschina.net/linwaiwai/blog/4696224)

[大厂推荐使用的网关解密：FizzGate管理后台使用教程](https://my.oschina.net/linwaiwai/blog/4696124)

[架构师效率快的终极原因：FizzGate网关之服务编排](https://my.oschina.net/linwaiwai/blog/4696116)

[高阶架构师支招：FizzGate的插件开发](https://my.oschina.net/linwaiwai/blog/4696131)

[高阶程序员必备技能：FizzGate网关的二次开发](https://my.oschina.net/linwaiwai/blog/4696133)

[FizzGate网关入门教程-安装](https://zhuanlan.zhihu.com/p/501305059)

[FizzGate网关入门教程-路由初体验](https://zhuanlan.zhihu.com/p/501381970)

[FizzGate网关入门教程-权限校验](https://zhuanlan.zhihu.com/p/501384396)

[FizzGate网关入门教程-快速聚合多接口，提高页面数据的加载速度](https://zhuanlan.zhihu.com/p/501387154)

[FizzGate网关入门教程-服务编排，祭出终结BFF层的大杀器](https://zhuanlan.zhihu.com/p/501389075)

[企业级微服务API网关FizzGate-常用插件介绍](https://zhuanlan.zhihu.com/p/513656382)

[企业级微服务API网关FizzGate-如何自定义插件](https://zhuanlan.zhihu.com/p/513662893)

[企业级微服务API网关FizzGate-服务编排内置函数](https://zhuanlan.zhihu.com/p/513404417)

[FizzGate企业级微服务API网关进阶系列教程-服务编排处理列表数据(上)-展开与合并](https://zhuanlan.zhihu.com/p/515056309)

[FizzGate企业级微服务API网关进阶系列教程-服务编排处理列表数据(中)-数据提取与数据关联](https://zhuanlan.zhihu.com/p/515070075)

[FizzGate企业级微服务API网关进阶系列教程-服务编排处理列表数据(下)-字段重命名&字段移除](https://zhuanlan.zhihu.com/p/515509832)

## Authorization instructions

1. The fizz-gateway-node community version of the gateway core project is opened in the form of GNU V3 and can be used free of charge in non-commercial projects following the GNU protocol.

2. Management backend projects (fizz-manager-professional) as commercial versions only open binary packages [free download](https://wj.qq.com/s2/8682608/8fe2/), and For commercial projects, please contact us (sale@fizzgate.com) for authorization.

## System screenshot


![homepage](https://cdn.fizzgate.com/fizz/assets/img/manager_source_statistics_1.991ec114.png)

![aggr1](https://cdn.fizzgate.com/fizz/assets/img/manager_aggregate_add_2.72b385b5.png)

![aggr2](https://cdn.fizzgate.com/fizz/assets/img/manager_aggregate_add_9.662f119e.png)

![route](https://cdn.fizzgate.com/fizz/assets/img/route1.1fd8abd1.png)

![plugin](https://cdn.fizzgate.com/fizz/assets/img/manager_plugin_add_2.e1b5a24e.png)

![appid](https://cdn.fizzgate.com/fizz/assets/img/manager_app_id_add_2.49208bf6.png)

![breaker](https://cdn.fizzgate.com/fizz/assets/img/component2.7e77c716.png)

![flowcontrol](https://cdn.fizzgate.com/fizz/assets/img/manager_flow_control_rule_default_edit_2.130223a7.png)

![doc](https://cdn.fizzgate.com/fizz/assets/img/manager_interface_collection_preview_2.eee99e97.png)
