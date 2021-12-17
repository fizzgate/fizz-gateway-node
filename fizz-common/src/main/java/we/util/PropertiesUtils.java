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

    public static String normalize(String propertyName) {
        char[] chars = propertyName.toCharArray();
        StringBuilder b = new StringBuilder(chars.length);
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == Consts.S.DASH) {
                b.append(Character.toUpperCase(chars[++i]));
            } else {
                b.append(c);
            }
        }
        return b.toString();
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
                beanWrapper.setPropertyValue(propertyName, properties.get(propertyName));
            } else if (propertyTypeHint != null) {
                int dotPos = propertyName.lastIndexOf(Consts.S.DOT);
                if (dotPos > -1) {
                    String prefix = propertyName.substring(0, dotPos);
                    Class<?> aClass = propertyTypeHint.get(prefix);
                    if (aClass != null && Map.class.isAssignableFrom(aClass)) {
                        // String newPropertyName = StringUtils.replaceChars(propertyName, Consts.S.DOT, PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR);
                        String newPropertyName = prefix + PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR + propertyName.substring(dotPos + 1) + PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR;
                        // newPropertyName = newPropertyName + PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR;
                        beanWrapper.setPropertyValue(newPropertyName, properties.get(propertyName));
                    }
                }
            }
        }
    }
}
