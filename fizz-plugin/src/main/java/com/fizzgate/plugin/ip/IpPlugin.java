package com.fizzgate.plugin.ip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fizzgate.plugin.ip.util.ConfigUtils;
import com.fizzgate.plugin.ip.util.IpMatchUtils;
import com.fizzgate.plugin.ip.util.IpUtils;
import com.fizzgate.plugin.FizzPluginFilter;
import com.fizzgate.plugin.FizzPluginFilterChain;
import com.fizzgate.plugin.auth.ApiConfig;
import com.fizzgate.plugin.auth.ApiConfigService;
import com.fizzgate.util.WebUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.SocketException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.fizzgate.plugin.ip.RouterConfig.FieldName.*;

/**
 * @author hua.huang
 */
@Slf4j
@Component(value = IpPlugin.PLUGIN_NAME)
public class IpPlugin implements FizzPluginFilter {
    public static final String PLUGIN_NAME = "fizz_plugin_ip";
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private ApiConfigService apiConfigService;

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Void> filter(ServerWebExchange exchange, Map<String, Object> config) {
        RouterConfig routerConfig = routerConfig(exchange, config);
        List<PluginConfig.Item> pluginConfigItemList = null;/* pluginConfig(exchange, config); */
        if (access(exchange, routerConfig, pluginConfigItemList)) {
            log.trace("pass...");
            return FizzPluginFilterChain.next(exchange);
        }
        log.trace("forbidden!");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, routerConfig.getErrorRespContentType());
        return WebUtils.buildDirectResponse(exchange, HttpStatus.FORBIDDEN,
                headers, routerConfig.getErrorRespContent());
    }

    private boolean access(ServerWebExchange exchange,
                           RouterConfig routerConfig, List<PluginConfig.Item> pluginConfigItemList) {
        Set<String> fixedWhiteIpSet = Sets.newHashSet();
        Set<String> fixedBlackIpSet = Sets.newHashSet();
        ApiConfig apiConfig = apiConfig(exchange);
        Set<String> gatewayGroups = (apiConfig == null || apiConfig.gatewayGroups == null) ? Sets.newHashSet() : apiConfig.gatewayGroups;
        /* if (!CollectionUtils.isEmpty(pluginConfigItemList)) {
            for (PluginConfig.Item fixedConfigItem : pluginConfigItemList) {
                if (gatewayGroups.contains(fixedConfigItem.getGwGroup())) {
                    fixedWhiteIpSet.addAll(IpMatchUtils.ipConfigList(fixedConfigItem.getWhiteIp()));
                    fixedBlackIpSet.addAll(IpMatchUtils.ipConfigList(fixedConfigItem.getBlackIp()));
                }
            }
        } */
        Set<String> whiteIpSet = ConfigUtils.string2set(routerConfig.getWhiteIp());
        Set<String> blackIpSet = ConfigUtils.string2set(routerConfig.getBlackIp());

        String ip = null;
        try {
            ip = IpUtils.getServerHttpRequestIp(exchange.getRequest());
        } catch (SocketException e) {
            log.warn(e.getMessage(), e);
        }
        log.trace("clientIp:{}, fixedWhiteIpSet:{}, fixedBlackIpSet:{}, whiteIpSet:{}, blackIpSet:{}",
                ip, fixedWhiteIpSet, fixedBlackIpSet, whiteIpSet, blackIpSet);
        // 未获取到client ip，返回false
        if (StringUtils.isBlank(ip)) {
            return false;
        }

        // 优先匹配路由级别配置，然后再匹配插件级别配置

        // 路由级别：：白名单匹配到就直接返回true
        if (IpMatchUtils.match(ip, whiteIpSet)) {
            return true;
        }
        // 路由级别：：黑名单匹配到就直接返回false
        if (IpMatchUtils.match(ip, blackIpSet)) {
            return false;
        }
        // 插件级别：：白名单匹配到就直接返回true
        /* if (IpMatchUtils.match(ip, fixedWhiteIpSet)) {
            return true;
        } */
        // 插件级别：：黑名单匹配到就直接返回false
        /* if (IpMatchUtils.match(ip, fixedBlackIpSet)) {
            return false;
        } */
        // 路由级别和插件级别都没匹配到
        if (CollectionUtils.isEmpty(whiteIpSet) /* || CollectionUtils.isEmpty(fixedWhiteIpSet) */) {
            // 都没有配置白名单，默认返回true
            return true;
        } else {
            return false;
        }
    }

    private RouterConfig routerConfig(ServerWebExchange exchange, Map<String, Object> config) {
        RouterConfig routerConfig = new RouterConfig();
        routerConfig.setErrorRespContentType((String) config.getOrDefault(ERROR_RESP_CONTENT_TYPE
                , routerConfig.getErrorRespContentType()));
        routerConfig.setErrorRespContent((String) config.getOrDefault(ERROR_RESP_CONTENT
                , routerConfig.getErrorRespContent()));
        routerConfig.setWhiteIp((String) config.getOrDefault(WHITE_IP, StringUtils.EMPTY));
        routerConfig.setBlackIp((String) config.getOrDefault(BLACK_IP, StringUtils.EMPTY));
        return routerConfig;
    }

    private List<PluginConfig.Item> pluginConfig(ServerWebExchange exchange, Map<String, Object> config) {
        String fixedConfig = (String) config.get(com.fizzgate.plugin.PluginConfig.CUSTOM_CONFIG);
        try {
            PluginConfig pluginConfig = objectMapper.readValue(fixedConfig, PluginConfig.class);
            if (pluginConfig != null && pluginConfig.getConfigs() != null) {
                return pluginConfig.getConfigs();
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return Lists.newArrayList();
    }

    private ApiConfig apiConfig(ServerWebExchange exchange) {
        ServerHttpRequest req = exchange.getRequest();
        return apiConfigService.getApiConfig(WebUtils.getAppId(exchange),
                WebUtils.getClientService(exchange), req.getMethod(), WebUtils.getClientReqPath(exchange));
    }

}
