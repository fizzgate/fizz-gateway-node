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

package we.config;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import we.beans.factory.config.FizzBeanFactoryPostProcessor;
import we.beans.factory.config.FizzBeanPostProcessor;
import we.context.event.FizzRefreshEventListener;
import we.context.scope.refresh.FizzRefreshScope;

/**
 * @author hongqiaowei
 */

@Configuration(proxyBeanMethods = false)
public class FizzConfigConfiguration {

    public static final String REFRESH_SCOPE_NAME = "fizz-refresh";

    @Bean
    public FizzBeanFactoryPostProcessor fizzBeanFactoryPostProcessor() {
        return new FizzBeanFactoryPostProcessor();
    }

    /*@Bean
    public FizzBeanPostProcessor fizzBeanPostProcessor() {
        return new FizzBeanPostProcessor();
    }*/

    @Bean
    public static FizzRefreshScope fizzRefreshScope() {
        return new FizzRefreshScope();
    }

    @Bean
    public FizzRefreshEventListener fizzRefreshEventListener(ConfigurableApplicationContext context, FizzRefreshScope scope) {
        return new FizzRefreshEventListener(scope);
    }
}
