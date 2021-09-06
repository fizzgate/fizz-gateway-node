package we;

import org.springframework.context.annotation.Configuration;
import we.config.ManualApiConfig;
import we.plugin.PluginConfig;
import we.plugin.auth.ApiConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 定义 MyApiConfig 继承 ManualApiConfig， 并注解为 Configuration，
 * 然后实现 setApiConfigs 方法，在方法中添加路由配置
 */
@Configuration
public class MyApiConfig extends ManualApiConfig {

    @Override
    public List<ApiConfig> setApiConfigs() {

        List<ApiConfig> apiConfigs = new ArrayList<>();

        ApiConfig ac = new ApiConfig(); // 一个路由配置
        ac.id = 1000; // 路由 id，建议从 1000 开始
        ac.service = "xservice"; // 前端服务名
        ac.path = "/ypath"; // 前端路径
        ac.type = ApiConfig.Type.REVERSE_PROXY; // 路由类型，此处为反向代理
        ac.httpHostPorts = Collections.singletonList("http://127.0.0.1:9094"); // 被代理接口的地址
        // ac.httpHostPorts = Collections.singletonList("https://self-signed.badssl.com");
        ac.backendPath = "/@ypath"; // 被代理接口的路径
        // ac.backendPath = "/";
        ac.pluginConfigs = new ArrayList<>();
        PluginConfig pc = new PluginConfig();
        pc.plugin = "myPlugin"; // 应用 id 为 myPlugin 的插件
        ac.pluginConfigs.add(pc);

        apiConfigs.add(ac);

        log.info("set api configs end");
        return apiConfigs; // 返回路由配置
    }
}
