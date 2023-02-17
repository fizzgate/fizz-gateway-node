package com.fizzgate.plugin.core.filter.config.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fizzgate.plugin.core.filter.config.FizzConfig;
import com.fizzgate.plugin.core.filter.config.parser.JsonParser;
import com.google.common.collect.Maps;
import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JsonParserTest {

    private static JsonParser parser = new JsonParser();

    @BeforeAll
    public static void init() throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = parser.getClass();
        Field field = clazz.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(parser, new ObjectMapper());
    }

    @Test
    void parseRouterCfg() {
        String varJson = "{\n" +
                "  \"var1\": \"var1\",\n" +
                "  \"var2\": \"var2\",\n" +
                "  \"var3\": \"var3\"\n" +
                "}";
//        String varJson = "";
//        String varJson = null;
        Map<String, Object> config = Maps.newHashMap();
        config.put("codeSource", "this is code source");
        config.put("var", varJson);
        RouterConfig routerConfig = parser.parseRouterCfg(config, RouterConfig.class);
        assertNotNull(routerConfig, "未解析出routerConfig");
        assertNotNull( routerConfig.getVar(), "未解析出routerConfig.var");
        assertEquals("var1", routerConfig.getVar().getVar1(), "routerConfig.var.var1不匹配");

    }

    @Test
    void parsePluginCfg() {
        String json = "{\n" +
                "  \"id\": \"123\",\n" +
                "  \"var\": {\n" +
                "    \"var1\": \"var1\",\n" +
                "    \"var2\": \"var2\",\n" +
                "    \"var3\": \"var3\"\n" +
                "  }\n" +
                "}";
        PluginConfig config = parser.parsePluginCfg(json, PluginConfig.class);
        assertNotNull(config, "未解析出config");
        assertEquals("123", config.getId(), "id不匹配");
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