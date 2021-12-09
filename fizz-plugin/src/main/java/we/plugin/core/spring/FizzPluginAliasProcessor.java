package we.plugin.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.CollectionUtils;
import we.plugin.core.filter.AbstractFizzPlugin;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * @author huanghua
 */
@Slf4j
public class FizzPluginAliasProcessor {
    private ApplicationContext applicationContext;

    public FizzPluginAliasProcessor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void postProcessPluginAlias() {
        Map<String, AbstractFizzPlugin> serviceBeanMap = applicationContext.getBeansOfType(AbstractFizzPlugin.class);
        if (CollectionUtils.isEmpty(serviceBeanMap)) {
            log.debug("not found fizz plugin. skip!");
            return;
        }
        if (!(applicationContext instanceof GenericApplicationContext)) {
            log.error("ApplicationContext is not instance of GenericApplicationContext. skip!");
            return;
        }
        serviceBeanMap.forEach((s, o) -> registerAlias(
                ((GenericApplicationContext) applicationContext).getBeanFactory(), s, o));
    }

    private void registerAlias(ConfigurableListableBeanFactory beanFactory,
                               String beanName, AbstractFizzPlugin fizzPlugin) {
        log.debug("register bean : {}", fizzPlugin.getClass().getName());
        BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
        if (bd instanceof AbstractBeanDefinition) {
            AbstractBeanDefinition abd = (AbstractBeanDefinition) bd;
            abd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_AUTODETECT);
        }
        beanFactory.registerAlias(beanName, fizzPlugin.pluginName());
    }
}
