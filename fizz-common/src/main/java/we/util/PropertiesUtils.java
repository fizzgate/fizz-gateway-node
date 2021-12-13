package we.util;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.propertyeditors.CustomMapEditor;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author hongqiaowei
 */

public abstract class PropertiesUtils {

    private PropertiesUtils() {
    }

    public static Properties remove(Properties properties, String prefix) {
        Properties result = new Properties();
        properties.forEach(
                (k, v) -> {
                    String s = k.toString();
                    int idx = s.indexOf(prefix);
                    if (idx > -1) {
                        s = s.substring(prefix.length());
                    }
                    result.setProperty(s, v.toString());
                }
        );
        return result;
    }

    public static void set(Object bean, Properties properties) {
//      BeanWrapper beanWrapper = new BeanWrapperImpl(bean);

        BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean);
        System.err.println("");

        beanWrapper.registerCustomEditor(Map.class, "serviceUrl.", new CustomMapEditor(HashMap.class));

        beanWrapper.setPropertyValues(properties);
    }
}
