package com.fizzgate.plugin.core.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import com.fizzgate.Fizz;
import com.fizzgate.plugin.FizzPluginFilter;
import com.fizzgate.plugin.PluginConfig;
import com.fizzgate.plugin.auth.ApiConfig;
import com.fizzgate.plugin.auth.ApiConfigService;
import com.fizzgate.plugin.core.filter.config.ContentParser;
import com.fizzgate.plugin.core.filter.config.FizzConfig;
import com.fizzgate.plugin.core.filter.config.parser.JsonParser;
import com.fizzgate.util.WebUtils;

import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.function.Function;

/**
 * @author huanghua
 */
@Slf4j
@SuppressWarnings("unchecked")
public abstract class AbstractFizzPlugin<RouterCfg, PluginCfg> implements FizzPluginFilter {
    // api 配置
    public final Function<String, String> nameExApiCfg = in -> "fizz.pl.api.cfg";
    // 路由上的插件配置（原始）
    public final Function<String, String> nameExRtCfg = in -> "fizz.pl.rt.cfg." + pluginName();
    // 插件级别配置（原始）
    public final Function<String, String> nameExPlCfg = in -> "fizz.pl.pl.cfg." + pluginName();
    // 路由上的插件配置（解析后）
    public final Function<String, String> nameExRtCfgParsed = in -> "fizz.pl.rt.cfg.parsed." + pluginName();
    // 插件级别配置（解析后）
    public final Function<String, String> nameExPlCfgParsed = in -> "fizz.pl.pl.cfg.parsed." + pluginName();
    @Resource
    private ApiConfigService apiConfigService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, Map<String, Object> config) {
        if (log.isTraceEnabled()) {
            log.trace("{} doFilter execute...", this.pluginName());
        }
        initConfig(exchange, config);
        return this.doFilter(exchange);
    }

    /**
     * 获取路由级别插件配置
     */
    public RouterCfg routerConfig(ServerWebExchange exchange) {
        if (originRouterCfg(exchange) == null) {
            return null;
        }
        RouterCfg routerCfgInAttr = exchange.getAttribute(nameExRtCfgParsed.apply(pluginName()));
        if (routerCfgInAttr != null) {
            return routerCfgInAttr;
        }
        Class<RouterCfg> cfgClass = (Class<RouterCfg>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
        FizzConfig fizzConfig = AnnotationUtils.findAnnotation(cfgClass, FizzConfig.class);
        Class<? extends ContentParser> cfgParser = fizzConfig == null ? JsonParser.class : fizzConfig.contentParser();
        RouterCfg routerCfg = Fizz.context.getBean(cfgParser).parseRouterCfg(originRouterCfg(exchange), cfgClass);
        putAttr2exchange(exchange, nameExRtCfgParsed.apply(pluginName()), routerCfg);
        return routerCfg;
    }

    /**
     * 获取插件级别插件配置
     */
    public PluginCfg pluginConfig(ServerWebExchange exchange) {
        if (originPluginCfg(exchange) == null) {
            return null;
        }
        PluginCfg pluginCfgInAttr = exchange.getAttribute(nameExPlCfgParsed.apply(pluginName()));
        if (pluginCfgInAttr != null) {
            return pluginCfgInAttr;
        }
        Class<PluginCfg> cfgClass = (Class<PluginCfg>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[1];
        FizzConfig fizzConfig = AnnotationUtils.findAnnotation(cfgClass, FizzConfig.class);
        Class<? extends ContentParser> cfgParser = fizzConfig == null ? JsonParser.class : fizzConfig.contentParser();
        PluginCfg pluginCfg = Fizz.context.getBean(cfgParser).parsePluginCfg(originPluginCfg(exchange), cfgClass);
        putAttr2exchange(exchange, nameExPlCfgParsed.apply(pluginName()), pluginCfg);
        return pluginCfg;
    }

    /**
     * 获取原始路由级别插件配置
     */
    public <T> T originRouterCfg(ServerWebExchange exchange) {
        return exchange.getAttribute(nameExRtCfg.apply(pluginName()));
    }

    /**
     * 获取原始插件级别插件配置
     */
    public <T> T originPluginCfg(ServerWebExchange exchange) {
        return exchange.getAttribute(nameExPlCfg.apply(pluginName()));
    }

    /**
     * 获取路由配置
     */
    public ApiConfig apiConfig(ServerWebExchange exchange) {
        return exchange.getAttribute(nameExApiCfg.apply(pluginName()));
    }

    protected void putAttr2exchange(ServerWebExchange exchange, String key, Object val) {
        if (exchange == null || key == null || val == null) {
            return;
        }
        exchange.getAttributes().put(key, val);
    }

    private void initConfig(ServerWebExchange exchange, Map<String, Object> config) {
        ServerHttpRequest req = exchange.getRequest();
        ApiConfig apiConfig = apiConfigService.getApiConfig(WebUtils.getAppId(exchange),
                WebUtils.getClientService(exchange), req.getMethod(), WebUtils.getClientReqPath(exchange));
        String fixedConfig = (String) config.get(PluginConfig.CUSTOM_CONFIG);
        if (log.isTraceEnabled()) {
            log.trace("api config : {}", apiConfig);
            log.trace("router config : {}", config);
            log.trace("plugin config : {}", fixedConfig);
        }
        putAttr2exchange(exchange, nameExApiCfg.apply(pluginName()), apiConfig);
        putAttr2exchange(exchange, nameExRtCfg.apply(pluginName()), config);
        putAttr2exchange(exchange, nameExPlCfg.apply(pluginName()), fixedConfig);
    }

    /**
     * 插件名称
     */
    public abstract String pluginName();

    /**
     * filter逻辑
     */
    public abstract Mono<Void> doFilter(ServerWebExchange exchange);

}
