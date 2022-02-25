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

package we.context.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import we.beans.factory.config.FizzBeanFactoryPostProcessor;
import we.context.scope.refresh.FizzRefreshScope;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author hongqiaowei
 */

public class FizzRefreshEventListener implements SmartApplicationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FizzRefreshEventListener.class);

    private final FizzBeanFactoryPostProcessor fizzBeanFactoryPostProcessor;

    private final FizzRefreshScope             fizzRefreshScope;

    private final AtomicBoolean                ready                         = new AtomicBoolean(false);

    public FizzRefreshEventListener(FizzBeanFactoryPostProcessor fizzBeanFactoryPostProcessor, FizzRefreshScope fizzRefreshScope) {
        this.fizzBeanFactoryPostProcessor = fizzBeanFactoryPostProcessor;
        this.fizzRefreshScope = fizzRefreshScope;
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return ApplicationReadyEvent.class.isAssignableFrom(eventType) || FizzRefreshEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationReadyEvent) {
            handle((ApplicationReadyEvent) event);
        } else if (event instanceof FizzRefreshEvent) {
            handle((FizzRefreshEvent) event);
        }
    }

    public void handle(ApplicationReadyEvent event) {
        this.ready.compareAndSet(false, true);
    }

    public void handle(FizzRefreshEvent event) {
        if (this.ready.get()) {
            // EnvironmentChangeEvent ?
            if (event.getType() == FizzRefreshEvent.ENV_CHANGE) {
                /*Map<String*//*bean*//*, Map<String*//*property*//*, String*//*value*//*>> bean2propertyValuesMap = new HashMap<>();
                Map<String, String> changedPropertyValueMap = (Map<String, String>) event.getData();
                changedPropertyValueMap.forEach(
                        (property, value) -> {
                            String bean = fizzBeanFactoryPostProcessor.getBean(property);
                            if (bean != null) {
                                Map<String, String> propertyValueMap = bean2propertyValuesMap.computeIfAbsent(bean, k -> new HashMap<>());
                                propertyValueMap.put(property, value);
                            }
                        }
                );
                bean2propertyValuesMap.forEach(
                        (bean, propertyValueMap) -> {
                            fizzRefreshScope.refresh(bean);
                            LOGGER.info("fizz refresh {} bean with {}", bean, propertyValueMap);
                        }
                );*/
                Map<String, Object> changedPropertyValue = (Map<String, Object>) event.getData();
                changedPropertyValue.forEach(
                        (property, value) -> {
                            String bean = fizzBeanFactoryPostProcessor.getBean(property);
                            if (bean != null) {
                                fizzRefreshScope.refresh(bean);
                                LOGGER.info("fizz refresh {} bean with {}={}", bean, property, value);
                            }
                        }
                );
            }
        }
    }

}
