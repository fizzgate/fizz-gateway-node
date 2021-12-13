package we.util;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.Properties;

/**
 * @author hongqiaowei
 */

public abstract class YmlUtils {

    private YmlUtils() {
    }

    public static Properties file2properties(String file) {
        Resource resource = new ClassPathResource(file);
        return getProperties(resource);
    }

    public static Properties string2properties(String config) {
        Resource resource = new ByteArrayResource(config.getBytes());
        return getProperties(resource);
    }

    private static Properties getProperties(Resource resource) {
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(resource);
        return yamlPropertiesFactoryBean.getObject();
    }
}
