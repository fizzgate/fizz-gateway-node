package com.fizzgate.plugin.ip;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

@Data
public class PluginConfig {
    private List<Item> configs = Lists.newArrayList();

    @Data
    public static class Item {
        private String gwGroup;
        private String whiteIp;
        private String blackIp;
    }
}
