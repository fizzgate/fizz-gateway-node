package com.fizzgate.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;

import com.fizzgate.util.PropertiesUtils;
import com.fizzgate.util.YmlUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class YmlUtilsTest {

    @Test
    void config2bean() throws IOException {
        String s = FileUtil.readString("eureka.yml", CharsetUtil.CHARSET_UTF_8);
        Properties properties = YmlUtils.string2properties(s);
        // System.err.println("properties: \n" + properties);
        properties = PropertiesUtils.remove(properties, "eureka.client");
        EurekaClientConfigBean eurekaClientConfig = new EurekaClientConfigBean();
        Map<String, Class<?>> propertyTypeHint = new HashMap<>();
        propertyTypeHint.put("serviceUrl", Map.class);
        PropertiesUtils.setBeanPropertyValue(eurekaClientConfig, properties, propertyTypeHint);
        // System.err.println(JacksonUtils.writeValueAsString(eurekaClientConfig));

        // ClassPathResource resource = new ClassPathResource("application.properties");
        // properties = PropertiesLoaderUtils.loadProperties(resource);
    }
}
