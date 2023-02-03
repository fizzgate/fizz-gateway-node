---
home: false
title: plugin core
---

## 主要封装说明

- 1、引入核心包后，不影响以前编写的插件，只是提供更加便捷的开发方式；
- 2、插件名字编写规范。在保留原有编写方式的前提下，强制实现 pluginName() 方法，对开发更加友好，减少失误；
- 3、配置获取更加容易。配置主要有3个：路由配置、插件全局配置、插件在路由里的个性配置。现在都可以直接获取相应的配置实体对象，而不是默认提供的 Map 或者 String ；

## 使用说明

核心包是用于开发插件的基础包。主要是简化操作，方便开发，使开发人员更专注于业务代码对编写。

**1、编写2个配置实体类：插件在路由里的个性配置、插件全局配置，并添加注解 @FizzConfig**

> @FizzConfig 参数说明：
>
> contentParser ：配置内容解析器。选填，默认是 json 解析器 JsonParser 。也可以自定义解析器，只需实现 com.fizzgate.plugin.core.filter.config.ContentParser 接口

注意：默认解析器 JsonParser 的 parseRouterCfg 方法只对第一层的 json string 做了增强，但这也足够用了。如（注意 varJson 是个 json 字符串，并不是 json 对象）：
```groovy
void parseRouterCfg() {
        String varJson = "{\n" +
                "  \"var1\": \"var1\",\n" +
                "  \"var2\": \"var2\",\n" +
                "  \"var3\": \"var3\"\n" +
                "}";
//        String varJson = "";
//        String varJson = null;
        Map<String, String> config = Maps.newHashMap();
        config.put("codeSource", "this is code source");
        config.put("var", varJson);
        RouterConfig routerConfig = parser.parseRouterCfg(config, RouterConfig.class);
        System.out.println(routerConfig);
    }
```

示例：

```java
    @Data
    @FizzConfig
    public class PluginConfig {
        private String id;
        private Var var;
    }

    @Data
    @FizzConfig
    public class RouterConfig {
        private String codeSource;
        private Var var;
    }

    @Data
    public class Var {
        private String var1;
        private Integer var2;
        private Long var3;
    }
```

**3、编写插件逻辑**
继承 com.fizzgate.plugin.core.filter.AbstractFizzPlugin ，并实现 pluginName 和 doFilter 方法

> pluginName 方法：获取插件名称。无参，返回插件名称，要与网关后台配置的插件名称一致
>
> doFilter 方法：插件主要逻辑方法。入参是 ServerWebExchange，出参是 Mono<Void>

直接调用父类方法获取各种配置：
> routerConfig：获取路由级别插件配置
>
> pluginConfig：获取插件级别插件配置
>
> originRouterCfg：获取原始路由级别插件配置
>
> originPluginCfg：获取原始插件级别插件配置
>
> apiConfig：获取路由配置

示例：

```java
package com.fizzgate.fizz.plugin.example.plugin;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.fizzgate.plugin.core.filter.AbstractFizzPlugin;
import com.fizzgate.plugin.core.filter.config.FizzConfig;
import com.fizzgate.plugin.auth.ApiConfig;
import com.fizzgate.util.WebUtils;

import java.util.Map;

import static com.fizzgate.fizz.plugin.example.plugin.ExamplePlugin.PluginConfig;
import static com.fizzgate.fizz.plugin.example.plugin.ExamplePlugin.RouterConfig;

@Slf4j
@Component
public class ExamplePlugin extends AbstractFizzPlugin<RouterConfig, PluginConfig> {

    /**
     * 插件名称
     */
    @Override
    public String pluginName() {
        return "examplePlugin";
    }

    /**
     * filter逻辑
     */
    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange) {
        RouterConfig routerConfig = routerConfig(exchange);
        PluginConfig pluginConfig = pluginConfig(exchange);
        Map<String, Object> originRouterCfg = originRouterCfg(exchange);
        String originPluginCfg = originPluginCfg(exchange);
        ApiConfig apiConfig = apiConfig(exchange);
        if (log.isTraceEnabled()) {
            log.trace("routerConfig : {}", routerConfig);
            log.trace("pluginConfig : {}", pluginConfig);
            log.trace("originRouterCfg : {}", originRouterCfg);
            log.trace("originPluginCfg : {}", originPluginCfg);
            log.trace("apiConfig : {}", apiConfig);
        }
        return WebUtils.buildDirectResponse(exchange, HttpStatus.OK, null, "success");
    }

    @Data
    @FizzConfig
    public static class PluginConfig {
        private String id;
        private Var var;
    }

    @Data
    public static class Var {
        private String var1;
        private String var2;
        private String var3;
    }

    @Data
    @FizzConfig
    public static class RouterConfig {
        private String codeSource;
        private Var var;
    }
}

```


