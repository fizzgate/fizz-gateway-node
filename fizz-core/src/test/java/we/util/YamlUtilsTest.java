package we.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class YamlUtilsTest {

    // @Test
    void dumpJavaObject() throws IOException {
        InetUtilsProperties inetUtilsProperties = new InetUtilsProperties();
        InetUtils inetUtils = new InetUtils(inetUtilsProperties);

        EurekaInstanceConfigBean eurekaInstanceConfigBean = new EurekaInstanceConfigBean(inetUtils);
        eurekaInstanceConfigBean.setAppname("abc");

        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("md1", "val1");
        eurekaInstanceConfigBean.setMetadataMap(metadataMap);

        MyDataCenterInfo myDataCenterInfo = new MyDataCenterInfo(DataCenterInfo.Name.MyOwn);
        eurekaInstanceConfigBean.setDataCenterInfo(myDataCenterInfo);
    }

    @Test
    void config2bean() throws IOException {
        String s = FileUtil.readString("test.yml", CharsetUtil.CHARSET_UTF_8);
        Properties properties = YmlUtils.string2properties(s);
        // System.err.println("properties: \n" + properties);
        properties = PropertiesUtils.remove(properties, "eureka.client");
        EurekaClientConfigBean eurekaClientConfig = new EurekaClientConfigBean();
        Map<String, Class<?>> propertyTypeHint = new HashMap<>();
        propertyTypeHint.put("serviceUrl", Map.class);
        PropertiesUtils.setBeanPropertyValue(eurekaClientConfig, properties, propertyTypeHint);
        // System.err.println(JacksonUtils.writeValueAsString(eurekaClientConfig));

        ClassPathResource resource = new ClassPathResource("application.properties");
        properties = PropertiesLoaderUtils.loadProperties(resource);
    }
}
