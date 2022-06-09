/*
 *  Copyright (C) 2020 the original author or authors.
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

package we.dedicated_line;

import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import we.config.SystemConfig;
import we.service_registry.FizzServiceRegistration;
import we.service_registry.eureka.FizzEurekaHelper;
import we.service_registry.nacos.FizzNacosHelper;

import javax.annotation.PreDestroy;
import java.util.Properties;

/**
 * @author hongqiaowei
 */

@Configuration
@ConditionalOnDiscoveryEnabled
@ConditionalOnProperty(name = SystemConfig.FIZZ_DEDICATED_LINE_CLIENT_ENABLE, havingValue = "true")
@AutoConfigureAfter({EurekaClientAutoConfiguration.class, NacosDiscoveryAutoConfiguration.class})
public class DedicatedLineServiceRegistration implements ApplicationListener<DedicatedLineWebServerInitializedEvent> {

    private static final Logger log = LoggerFactory.getLogger(DedicatedLineServiceRegistration.class);

    private FizzServiceRegistration fizzServiceRegistration;

    @Value("${fizz.dedicated-line.client.enable:true}")
    private boolean fizzDedicatedLineClientEnable;

    @SneakyThrows
    @Override
    public void onApplicationEvent(DedicatedLineWebServerInitializedEvent event) {

        if (!fizzDedicatedLineClientEnable) {
            return;
        }

        ReactiveWebServerApplicationContext applicationContext = event.getApplicationContext();
        ConfigurableEnvironment env = applicationContext.getEnvironment();

        String prefix  = SystemConfig.FIZZ_DEDICATED_LINE_CLIENT_PREFIX + ".service-registration";
        boolean eureka = env.containsProperty((prefix + ".eureka.client.serviceUrl.defaultZone"));
        boolean nacos  = env.containsProperty((prefix + ".nacos.discovery.server-addr"));

        if (eureka || nacos) {
            if (eureka) {
                Properties eurekaProperties = new Properties();
                boolean find = false;
                for (PropertySource<?> propertySource : env.getPropertySources()) {
                    // if (propertySource instanceof OriginTrackedMapPropertySource) {
                    if (MapPropertySource.class.isAssignableFrom(propertySource.getClass())) {
                        MapPropertySource originTrackedMapPropertySource = (MapPropertySource) propertySource;
                        String[] propertyNames = originTrackedMapPropertySource.getPropertyNames();
                        for (String propertyName : propertyNames) {
                            if (propertyName.length() > 55) {
                                int eurekaPos = propertyName.indexOf("eureka");
                                if (eurekaPos > -1) {
                                    eurekaProperties.setProperty(propertyName.substring(eurekaPos), originTrackedMapPropertySource.getProperty(propertyName).toString());
                                    find = true;
                                }
                            }
                        }
                        if (find) {
                            break;
                        }
                    }
                }
                if (!find) {
                    log.error("no eureka config");
                    return;
                }
                fizzServiceRegistration = FizzEurekaHelper.getServiceRegistration(applicationContext, eurekaProperties);
            }

            if (nacos) {
                Properties nacosProperties = new Properties();
                boolean find = false;
                for (PropertySource<?> propertySource : env.getPropertySources()) {
                    // if (propertySource instanceof OriginTrackedMapPropertySource) {
                    if (MapPropertySource.class.isAssignableFrom(propertySource.getClass())) {
                        MapPropertySource originTrackedMapPropertySource = (MapPropertySource) propertySource;
                        String[] propertyNames = originTrackedMapPropertySource.getPropertyNames();
                        for (String propertyName : propertyNames) {
                            if (propertyName.length() > 64) {
                                int naocsPos = propertyName.indexOf("nacos");
                                if (naocsPos > -1) {
                                    nacosProperties.setProperty(propertyName.substring(naocsPos), originTrackedMapPropertySource.getProperty(propertyName).toString());
                                    find = true;
                                }
                            }
                        }
                        if (find) {
                            break;
                        }
                    }
                }
                if (!find) {
                    log.error("no nacos config");
                    return;
                }
                fizzServiceRegistration = FizzNacosHelper.getServiceRegistration(applicationContext, nacosProperties);
            }

            fizzServiceRegistration.register();
        }
    }

    @PreDestroy
    public void stop() {
        if (fizzServiceRegistration != null) {
            fizzServiceRegistration.deregister();
        }
    }
}
