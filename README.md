[English](./README.en-us.md) | 简体中文
<p align="center" >
    <a href="https://www.fizzgate.com"><img src="https://www.fizzgate.com/fizz/nav-bar/logo.png?v=1" width="70%"></a>
</p>
<p>
  <img alt="Version" src="https://img.shields.io/badge/version-3.1.0-blue.svg?cacheSeconds=2592000" />
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

- **商务及技术交流**:
请[直接购买](https://l.fizzgate.com/#/shop/purchase)或者[联系我们](https://www.fizzgate.com/fizz/cms/article/about/contact/)

为了给大家提供更好的服务，社区版本仅供测试，不提供任何技术咨询，商业版本请添加企业微信获取商业支持。

## FizzGate是什么？

An Aggregation API Gateway in Java . FizzGate 是一个基于 Java开发的微服务聚合网关，是拥有自主知识产权的应用网关国产化替代方案，能够实现热服务编排聚合、自动授权选择、线上服务脚本编码、在线测试、高性能路由、API审核管理、回调管理等目的，拥有强大的自定义插件系统可以自行扩展，并且提供友好的图形化配置界面，能够快速帮助企业进行API服务治理、减少中间层胶水代码以及降低编码投入、提高 API 服务的稳定性和安全性。

## 官方网站

https://www.fizzgate.com/

## 演示环境（Demo）

https://demo.fizzgate.com/

账号/密码: FizzGate是一款付费软件，如有需求，可直接购买或者添加企业微信免费试用

健康检查地址：https://demo.fizzgate.com/admin/health (线上版本请限制admin路径的外网访问)

API地址：https://demo.fizzgate.com/proxy/[服务名]/[API_Path]

## FizzGate的设计

<img width="500" src="https://user-images.githubusercontent.com/184315/97130741-33a90d80-177d-11eb-8680-f589a36e44b3.png" />

## FizzGate典型应用场景

<img width="90%" src="https://user-images.githubusercontent.com/6129661/216249866-71eb54de-d2e8-44ce-8e70-a1ca1f51553d.png" />

## 设计理念
界面智能，依赖最小，功能全面，部署方便。力求简洁、清晰的界面设计，尽量将复杂的配置步骤和操作简化，以适应不同层级的用户需求，让开发人员、运维人员等都能够高效地进行管理和配置。

- 界面智能：FizzGate 注重智能化设计，致力于提升用户体验。在用户输入时，系统将尽可能提供自动输入功能，减少用户操作的复杂度。同时，系统会在用户输入附近实时提供相关提示和提醒，帮助用户快速、准确地进行配置和操作。
- 依赖最小：FizzGate 在设计时，谨慎引入外部中间件和第三方依赖，对现有系统完全无侵入。大多数功能基于自研技术实现，最大程度地减少对外部依赖的需求，减少部署资源消耗的同时，保证系统的高效性和可维护性。这种设计使得FizzGate更加稳定，并且能在企业内部网络中更加灵活地部署。
- 功能全面：FizzGate 覆盖了整个 API 生命周期的管理，支持从 API 定义、集成、脱敏、溯源到安全防护等各个环节，全面提升API管理的效率。作为市场上功能最全面的产品之一，FizzGate 提供了一整套企业级解决方案，帮助企业实现高效、安全的API管理。
- 部署方便：FizzGate 在设计时充分考虑了企业级部署的需求，既支持小微企业的容灾需求，也能满足大型企业集群部署、异地多活部署的要求。部署过程简单快捷，最多一分钟即可解决企业的部署难题。

## 支持架构
支持arm64/amd64/x86/x86_64架构，可以在Mac、Window、Linux使用，支持信创，使用麒麟V10，达梦V8等主流信创组件。

## 产品特性
API管理
- 应用管理：支持对接入的应用进行管理；
- API管理：支持API定义后端服务的配置；
- 请求日志：支持全量记录API请求日志；
- 分组管理：支持通过分组管理实现同一分组的API使用相关的配置；
- 服务鉴权：通过插件可对服务进行应用访问权限、检验等链式的拦截策略；
- 集群管理：FizzGate网关节点是无状态的，配置信息自动同步，支持节点水平拓展和多集群部署；
- 安全授权：支持内置的key-auth, JWT, basic-auth授权方式，并且可以方便控制；
- 负载均衡：支持round-robin负载均衡；
- 策略熔断：根据服务或者具体地址进行多种恢复策略熔断配置；
- 多注册中心：支持从Eureka或Nacos注册中心进行服务发现；
- 配置中心：支持接入apollo配置中心；
- HTTP反向代理：隐藏真实后端服务，支持 Rest API反向代理；
- 访问策略：支持不同策略访问不同的API、配置不同的鉴权等；
- 黑白名单：支持配置通过绑定黑、白名单限制访问；
- 自定义插件：强大的插件机制支持自由扩展；
- 可扩展：简单易用的插件机制方便扩展功能；
- 高性能：性能在众多网关之中表现优异；
- 管理后台：通过管理后台界面对网关集群进行各项配置；
- 重放管理：支持回调的管理、订阅、重放、以及日志；
- 多级限流：细颗粒度的限流方式包含服务限流，接口限流，APP_ID限流，IP限流；
- 微服务文档：企业级管理开放微服务文档管理，系统集成更方便；
- 公网专线：建立公网中受到完全保护的私有连接通道；
- 透明代理：支持透明代理方式串联

API集成
- 服务编排：支持HTTP、Dubbo、gRPC、Soap协议热服务编排能力，支持前后端编码，支持JSON/XML输出，随时随地更新API；
- 版本控制：支持操作的发布和多次回滚；
- 集成测试：提供API集成测试功能，确保API接口的可靠性和可用性；
- 发布审核：支持API编排发布的审核流程

API脱敏
- 分类分级：支持对不同敏感数据进行分类和分级管理，合理安排脱敏策略；
- API脱敏：支持对API中涉密数据脱敏，支持结构、非结构数据；
- 脱敏策略：支持可视化的脱敏策略配置，根据上下文执行脱敏策略；
- 涉敏识别：支持对API中的涉敏数据进行识别

API溯源
- 网页文件水印：支持对代理的页面添加明水印。支持HTML、WORD/EXCEL/PPT/PDF、PNG/JPEG/TIFF等类型；
- 泄露溯源：支持根据泄露的数据信息，追溯查询过相关数据的所有上下文及终端信息。支持JSON/HTML、WORD/EXCEL/PPT/PDF、PNG/JPEG/TIFF等类型；

API安全（旁路分析)
- 资产分析：对API安全资产进行分析和管理，生成API Spec，帮助检测潜在的安全风险；
- API敏感数据：支持API敏感数据检测功能，实时发现API中的敏感数据泄露风险；
- API威胁：支持API异常访问检测功能，及时发现异常行为并进行响应；
- API漏洞：支持对API接口的安全漏洞进行扫描、报告，提升API的安全性；
- API阻断：在发现异常时，支持自动或手动阻断API的访问，避免攻击扩散。

## 基准测试

我们将FizzGate与市面上主要的网关产品进行比较，使用相同的环境和条件，测试对象均为单个节点。Mock接口模拟20ms时延，报文大小约2K。

- Intel(R) Xeon(R) CPU E5-2650 v3 @ 2.30GHz * 4
- Linux version 3.10.0-957.21.3.el7.x86_64
- 8G RAM

|         分类          |         产品          | 600并发<br/>QPS     | 600并发<br/>90% Latency(ms) | 1000并发<br/>QPS     | 1000并发<br/>90% Latency(ms) |
| :------------------ | :------------------ | :-------: | :-------: | :-------: | :-------: |
| 后端服务 | 直接访问后端服务    | 23540| 32.19 | 27325| 52.09 |
| 流量网关 | kong <br/>v2.4.1 | 15662 | 50.87 | 17152 | 84.3 |
| 应用网关 | fizz-gateway-node <br/>v3.0.0 | 12206 | 65.76 | 12766 | 100.34 |
| 应用网关 | spring-cloud-gateway <br/>v2.2.9| 11323 | 68.57 | 10472 | 127.59 |
| 应用网关 | shenyu <br/>v2.3.0| 9284 | 92.98 | 9939 | 148.61 |

## 版本对照

- fizz-gateway-node： 节点端

- fizz-manager-professional：管理后台

| fizz-gateway-node | fizz-manager-professional |
|-------------------|---------------------------|
| v1.3.0            | v1.3.0                    |
| ...               | ...                       |
| v3.0.0            | v3.0.0                    |
| v3.1.0            | v3.1.0                    |
| v3.2.0            | v3.2.0                    |
| v3.3.0            | v3.3.0                    |
| v3.5.0            | v3.5.0                    |
| v3.5.1            | v3.5.1                    |
| v3.6.0            | v3.6.0                    |

3.0.0之前的版本已停止维护，请根据节点端的版本下载对应的管理后台版本

## 一键安装
```bash
wget https://gitee.com/fizzgate/fizz-gateway-node/raw/master/install.sh && bash install.sh
```
说明：一键安装包使用docker-compose进行安装，yml文件已经内置镜像代理。如果您已经下载过docker-compose.yml文件，请删除重新下载。在安装之前请增加代理镜像地址。

```bash
sudo tee /etc/docker/daemon.json <<EOF
{
    "registry-mirrors": ["https://hub.fizzgateway.com"]
}
EOF

sudo systemctl daemon-reload
sudo systemctl restart docker
```
​
## 手动安装

### 部署说明

[详细部署教程>>>](http://www.fizzgate.com/guide/installation/) 

### 安装依赖

安装以下依赖软件：

- Redis v2.8或以上版本
- MySQL v5.7或以上版本
- Apollo配置中心 (可选)
- Eureka v1.10.17或Nacos v2.0.4或以上版本(可选)

依赖的安装可参考详细部署教程

#### 一、安装管理后台

[下载](https://www.fizzgate.com/fizz/cms/article/download/last/) fizz-manager-professional 安装包

##### 管理后台（fizz-manager-professional）

说明：

1. 以下安装步骤出现的`{version}`表示所使用管理后台的版本号，例如`1.3.0`。

安装方式一：二进制安装包

1. 解压`fizz-manager-professional-{version}.zip`安装包
2. 首次安装执行`fizz-manager-professional-{version}-mysql.sql`数据库脚本，从低版本升级至高版本选择执行update目录下对应升级脚本
3. 修改`application-prod.yml`文件，将相关配置修改成部署环境的配置
4. Linux启动 执行 `chmod +x boot.sh` 命令给`boot.sh`增加执行权限；执行 `./boot.sh start` 命令启动服务，支持 start/stop/restart/status命令
5. Windows启动 执行`.\boot.cmd start` 命令启动服务，支持 start/stop/restart/status命令

安装方式二（v2.0.0或以上版本）：docker:

SQL脚本：解压fizz-manager-professional 安装包

首次安装执行`fizz-manager-professional-{version}-mysql.sql`数据库脚本，从低版本升级至高版本选择执行update目录下对应升级脚本（如有脚本则执行）

1. 下载对应版本的镜像：docker pull fizzgate/fizz-manager-professional:{version}
2. 通过环境变量方式修改redis配置、database配置（其它配置同理）并运行镜像
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

或通过映射目录方式使用外部配置文件和输出日志到宿主机, 配置文件可从安装包里获取，在宿主机创建fizz-manager-professional/config和fizz-manager-professional/logs目录，把application-prod.yml配置文件放置config下，在fizz-manager-professional目录下运行镜像

```sh
cd fizz-manager-professional
docker run --rm -d -p 8000:8000 \
-v $PWD/config:/opt/fizz-manager-professional/config \
-v $PWD/logs:/opt/fizz-manager-professional/logs fizzgate/fizz-manager-professional:{version}
```

服务启动后访问 http://{部署机器IP地址}:8000/#/login，使用超级管理员账户`admin`密码`Aa123!`登录

#### 二、安装fizz-gateway-node节点端

说明：

1. 支持配置中心：apollo、nacos，支持注册中心：eureka、nacos，详细配置方法查看application.yml文件。
2. 如果使用apollo配置中心，可把application.yml文件内容迁到配置中心（apollo上应用名为：fizz-gateway）；如果不使用apollo可去掉下面启动命令里的apollo参数。
3. 以下安装步骤出现的`{version}`表示所使用网关的版本号，例如`1.3.0`。

安装方式一：二进制安装包

1. 下载fizz-gateway-node的二进制安装包，解压修改application.yml配置文件里配置中心、注册中心、redis(redis配置需与管理后台一致)的配置
2. 根据需要修改boot.sh脚本的apollo连接，不使用apollo配置中心可跳过
3. Linux启动 执行 `./boot.sh start` 命令启动服务，支持 start/stop/restart/status命令
4. Windows启动 执行`.\boot.cmd start` 命令启动服务，支持 start/stop/restart/status命令

安装方式二：源码安装:

1. 本地clone仓库上的最新代码，修改application.yml配置文件里配置中心、注册中心、redis(redis配置需与管理后台一致)的配置
2. 在项目根目录fizz-gateway-node下执行Maven命令`mvn clean package install -DskipTests=true`
3. 在项目目录fizz-gateway-node/fizz-bootstrap下执行Maven命令`mvn clean package -DskipTests=true`
4. 进入fizz-gateway-node/fizz-bootstrap/target/fizz-gateway-node目录，执行 `./boot.sh start` 命令启动服务，支持 start/stop/restart/status命令

安装方式三（v2.0.0或以上版本）：docker: 
1. 下载对应版本的镜像：docker pull fizzgate/fizz-gateway-node:{version}
2. 通过环境变量方式修改redis配置（其它配置同理）并运行镜像
```sh
docker run --rm -d -p 8600:8600 \
-e "aggregate.redis.host={your redis host IP}" \
-e "aggregate.redis.port={your redis port}" \
-e "aggregate.redis.password={your redis password}" \
-e "aggregate.redis.database={your redis database}" \
fizzgate/fizz-gateway-node:{version}
```

或通过映射目录方式使用外部配置文件和输出日志到宿主机, 配置文件可从安装包或源码里获取，在宿主机创建fizz-gateway-node/config和fizz-gateway-node/logs目录，把application.yml和log4j2-spring.xml配置文件放置config下，在fizz-gateway-node目录下运行镜像

```sh
cd fizz-gateway-node
docker run --rm -d -p 8600:8600 \
-v $PWD/config:/opt/fizz-gateway-node/config \
-v $PWD/logs:/opt/fizz-gateway-node/logs fizzgate/fizz-gateway-node:{version}
```

最后访问网关，地址形式为：http://127.0.0.1:8600/[服务名]/[API_Path]


## 相关文章

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


## 授权说明

1. 网关核心项目fizz-gateway-node节点端本以GNU v3的方式进行的开放，仅供技术朋友学习，任何商业使用都需要经过我们授权。

2. 管理后台项目(fizz-manager-professional)作为商业版本仅开放二进制包 [免费下载](https://www.fizzgate.com/fizz/cms/article/download/last/)，而商业项目请注明公司名称联系我们（sale@fizzgate.com）进行授权，了解商业授权规则请点击[商业授权规则](https://github.com/fizzgate/fizz-gateway-node/wiki/%E5%95%86%E4%B8%9A%E6%8E%88%E6%9D%83)

3. 在选择FizzGate之前，我们强烈建议您先试用一下我们的DEMO站点，试用我们的产品，并且思考与自身的业务结合，并且考虑产品推行落地方式，在查阅我们的官网价格(https://www.fizzgate.com) 之后再进一步与我们联系。

## 系统截图

![homepage](https://cdn.fizzgate.com/fizz/assets/img/manager_source_statistics_1.991ec114.png)

![aggr1](https://cdn.fizzgate.com/fizz/assets/img/manager_aggregate_add_2.72b385b5.png)

![aggr2](https://cdn.fizzgate.com/fizz/assets/img/manager_aggregate_add_9.662f119e.png)

![route](https://cdn.fizzgate.com/fizz/assets/img/route1.1fd8abd1.png)

![plugin](https://cdn.fizzgate.com/fizz/assets/img/manager_plugin_add_2.e1b5a24e.png)

![appid](https://cdn.fizzgate.com/fizz/assets/img/manager_app_id_add_2.49208bf6.png)

![breaker](https://cdn.fizzgate.com/fizz/assets/img/component2.7e77c716.png)

![flowcontrol](https://cdn.fizzgate.com/fizz/assets/img/manager_flow_control_rule_default_edit_2.130223a7.png)

![doc](https://cdn.fizzgate.com/fizz/assets/img/manager_interface_collection_preview_2.eee99e97.png)
