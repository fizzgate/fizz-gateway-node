package com.fizzgate.plugin.myplugin;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fizzgate.plugin.FizzPluginFilter;
import com.fizzgate.plugin.FizzPluginFilterChain;

import reactor.core.publisher.Mono;

import java.util.Map;

@Component(MyPlugin.MY_PLUGIN) // 必须，且为插件 id
public class MyPlugin implements FizzPluginFilter {

    public static final String MY_PLUGIN = "myPlugin"; // 插件 id

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, Map<String, Object> config) {
        System.err.println("this is my plugin"); // 本插件只输出这个
        return FizzPluginFilterChain.next(exchange); // 执行后续逻辑
    }
}
