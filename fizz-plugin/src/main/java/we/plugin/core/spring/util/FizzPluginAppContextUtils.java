package we.plugin.core.spring.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author huanghua
 * @deprecated and use {@link we.Fizz} instead
 */
@Component
public class FizzPluginAppContextUtils implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        applicationContext = appContext;
    }

    public static <T> T getBean(Class<T> requiredType) throws BeansException {
        return FizzPluginAppContextUtils.getApplicationContext().getBean(requiredType);
    }

    public static ApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            String msg = "The applicationContext is not yet available. "
                    + "Please ensure that the spring applicationContext is completely created before calling this method!";
            throw new IllegalStateException(msg);
        }

        return applicationContext;
    }
}
