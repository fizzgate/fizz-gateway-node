package we.plugin.myplugin;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.plugin.auth.ApiConfig;
import we.plugin.core.filter.AbstractFizzPlugin;
import we.plugin.core.filter.config.FizzConfig;
import we.util.WebUtils;

import java.util.Map;

@Slf4j
@Component
public class MyPluginPlus extends AbstractFizzPlugin<MyPluginPlus.RouterConfig, MyPluginPlus.PluginConfig> {

    /**
     * 插件名称
     */
    @Override
    public String pluginName() {
        return "myPluginPlus";
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
        private PluginItem pluginItem;
    }

    @Data
    public static class PluginItem {
        private String p1;
        private String p2;
        private String p3;
    }

    @Data
    @FizzConfig
    public static class RouterConfig {
        private String codeSource;
        private RouterItem routerItem;
    }

    @Data
    public static class RouterItem {
        private String r1;
        private String r2;
        private String r3;
    }

}
