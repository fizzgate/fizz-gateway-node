/*
 *  Copyright (C) 2021 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package we.beans.factory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.cloud.context.scope.GenericScope;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import we.context.config.annotation.FizzRefreshScope;
import we.util.ReflectionUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author hongqiaowei
 */

public class FizzBeanFactoryPostProcessor implements BeanFactoryPostProcessor, EnvironmentAware, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(FizzBeanFactoryPostProcessor.class);

    private ConfigurableEnvironment environment;

    private Map<String, String>     property2beanMap = new HashMap<>(32);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String redisHost = environment.getProperty("aggregate.redis.host");
        MutablePropertySources propertySources = environment.getPropertySources();
        Map<String, Object> map = new HashMap<>();
        map.put("aggregate.redis.host", "172.25.62.191");
        MapPropertySource fizzPropertySource = new MapPropertySource("fizzPropertySource", map);
        propertySources.addFirst(fizzPropertySource);

        Iterator<String> beanNamesIterator = beanFactory.getBeanNamesIterator();
        while (beanNamesIterator.hasNext()) {
            String beanName = beanNamesIterator.next();
            if (beanName.startsWith(GenericScope.SCOPED_TARGET_PREFIX)) {
                AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) beanFactory.getBeanDefinition(beanName);
                try {
                    beanDefinition.resolveBeanClass(null);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                Class<?> beanClass = beanDefinition.getBeanClass();
                FizzRefreshScope an = beanClass.getAnnotation(FizzRefreshScope.class);
                if (an != null) {
                    ReflectionUtils.doWithFields(
                            beanClass,
                            field -> {
                                Value annotation = field.getAnnotation(Value.class);
                                if (annotation != null) {
                                    property2beanMap.put(annotation.value(), beanName);
                                }
                            }
                    );
                    ReflectionUtils.doWithMethods(
                            beanClass,
                            method -> {
                                Value annotation = method.getAnnotation(Value.class);
                                if (annotation != null) {
                                    property2beanMap.put(annotation.value(), beanName);
                                }
                            }
                    );
                }
            }
        }

        LOGGER.info("property to bean map: {}", property2beanMap); // {${pname:lancer}=scopedTarget.person}
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    public Map<String, String> getProperty2beanMap() {
        return property2beanMap;
    }
}
