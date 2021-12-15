package we.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.PropertyAccessor;

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
                        s = s.substring(prefix.length() + 1);
                    }
                    result.setProperty(s, v.toString());
                }
        );
        return result;
    }

    public static void setBeanPropertyValue(Object bean, Properties properties) {
        setBeanPropertyValue(bean, properties, null);
    }

    public static void setBeanPropertyValue(Object bean, Properties properties, Map<String, Class<?>> propertyTypeHint) {
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(bean);
        for (String propertyName : properties.stringPropertyNames()) {
            if (beanWrapper.isWritableProperty(propertyName)) {
                Object value = properties.get(propertyName);
                if (propertyTypeHint == null) {
                    beanWrapper.setPropertyValue(propertyName, value);
                } else {
                    int dotPos = propertyName.indexOf(Consts.S.DOT);
                    String prefix = propertyName;
                    if (dotPos > -1) {
                        prefix = propertyName.substring(0, dotPos);
                    }
                    Class<?> aClass = propertyTypeHint.get(prefix);
                    if (aClass != null && Map.class.isAssignableFrom(aClass)) {
                        String newPropertyName = StringUtils.replaceChars(propertyName, Consts.S.DOT, PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR);
                        newPropertyName = newPropertyName + PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR;
                        beanWrapper.setPropertyValue(newPropertyName, value);
                    } else {
                        beanWrapper.setPropertyValue(propertyName, value);
                    }
                }
            }
        }
    }
}
