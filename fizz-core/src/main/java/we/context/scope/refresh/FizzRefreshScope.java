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

package we.context.scope.refresh;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.context.scope.GenericScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import we.config.FizzConfigConfiguration;

/**
 * @author hongqiaowei
 */

@ManagedResource
public class FizzRefreshScope extends GenericScope implements ApplicationContextAware,
        ApplicationListener<ContextRefreshedEvent>, Ordered {

    private ApplicationContext     context;

    private BeanDefinitionRegistry registry;

    private boolean                eager     = true;

    private int                    order     = Ordered.LOWEST_PRECEDENCE - 100;

    public FizzRefreshScope() {
        super.setName(FizzConfigConfiguration.REFRESH_SCOPE_NAME);
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setEager(boolean eager) {
        this.eager = eager;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
            throws BeansException {
        this.registry = registry;
        super.postProcessBeanDefinitionRegistry(registry);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        start(event);
    }

    public void start(ContextRefreshedEvent event) {
        if (event.getApplicationContext() == this.context && this.eager
                && this.registry != null) {
            eagerlyInitialize();
        }
    }

    private void eagerlyInitialize() {
        for (String name : this.context.getBeanDefinitionNames()) {
            BeanDefinition definition = this.registry.getBeanDefinition(name);
            if (this.getName().equals(definition.getScope())
                    && !definition.isLazyInit()) {
                Object bean = this.context.getBean(name);
                if (bean != null) {
                    bean.getClass();
                }
            }
        }
    }

    @ManagedOperation(description = "Dispose of the current instance of bean name provided and force a refresh on next method execution.")
    public boolean refresh(String name) {
        if (!name.startsWith(SCOPED_TARGET_PREFIX)) {
            name = SCOPED_TARGET_PREFIX + name;
        }
        if (super.destroy(name)) {
            this.context.publishEvent(new FizzRefreshScopeRefreshedEvent(name));
            return true;
        }
        return false;
    }

    @ManagedOperation(description = "Dispose of the current instance of all beans in this scope and force a refresh on next method execution.")
    public void refreshAll() {
        super.destroy();
        this.context.publishEvent(new FizzRefreshScopeRefreshedEvent());
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

}
