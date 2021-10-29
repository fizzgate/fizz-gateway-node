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

package we.api.pairing;

import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import we.config.SystemConfig;
import we.service_registry.eureka.FizzEurekaHelper;
import we.service_registry.eureka.FizzEurekaProperties;
import we.service_registry.eureka.FizzEurekaServiceRegistration;
import we.service_registry.nacos.FizzNacosHelper;
import we.service_registry.nacos.FizzNacosProperties;
import we.service_registry.nacos.FizzNacosServiceRegistration;

import javax.annotation.PreDestroy;

/**
 * @author hongqiaowei
 */

@Configuration
@ConditionalOnDiscoveryEnabled
@ConditionalOnProperty(name = SystemConfig.FIZZ_API_PAIRING_CLIENT_ENABLE, havingValue = "true")
@AutoConfigureAfter({EurekaClientAutoConfiguration.class, NacosDiscoveryAutoConfiguration.class})
public class ApiPairingServiceRegistration implements ApplicationListener<FizzApiPairingWebServerInitializedEvent> {

    private ServiceRegistry serviceRegistry;

    private Registration    registration;

    @SneakyThrows
    @Override
    public void onApplicationEvent(FizzApiPairingWebServerInitializedEvent event) {

        ReactiveWebServerApplicationContext applicationContext = event.getApplicationContext();
        ConfigurableEnvironment env = applicationContext.getEnvironment();

        String prefix = SystemConfig.FIZZ_API_PAIRING_CLIENT_PREFIX + ".service-registration";
        String type = env.getProperty(prefix + ".type");

        if (StringUtils.isNotBlank(type)) {
            if ("eureka".equals(type)) {
                String application     = env.getProperty(prefix + ".application");
                String ipAddress       = env.getProperty(prefix + ".ip-address");
                String port            = env.getProperty(prefix + ".port");
                String preferIpAddress = env.getProperty(prefix + ".prefer-ip-address", "true");
                String serviceUrl      = env.getProperty(prefix + ".service-url");

                FizzEurekaProperties fizzEurekaProperties = new FizzEurekaProperties().applicationContext(applicationContext)
                                                                                      .appName(application)
                                                                                      .ipAddress(ipAddress)
                                                                                      .nonSecurePort(Integer.parseInt(port))
                                                                                      .preferIpAddress(Boolean.parseBoolean(preferIpAddress))
                                                                                      .serviceUrl(serviceUrl);
                FizzEurekaServiceRegistration fizzEurekaServiceRegistration = FizzEurekaHelper.getServiceRegistration(fizzEurekaProperties);

                serviceRegistry = fizzEurekaServiceRegistration.serviceRegistry;
                registration    = fizzEurekaServiceRegistration.registration;
            }

            if ("nacos".equals(type)) {
                String application = env.getProperty(prefix + ".application");
                String ipAddress   = env.getProperty(prefix + ".ip-address");
                String port        = env.getProperty(prefix + ".port");
                String serviceUrl  = env.getProperty(prefix + ".service-url");
                String namespace   = env.getProperty(prefix + ".namespace", "");
                String group       = env.getProperty(prefix + ".group", "DEFAULT_GROUP");
                String clusterName = env.getProperty(prefix + ".clusterName", "DEFAULT");

                FizzNacosProperties fizzNacosProperties = new FizzNacosProperties();
                fizzNacosProperties.setApplicationContext(applicationContext);
                fizzNacosProperties.setId(application + ':' + serviceUrl);
                fizzNacosProperties.setService(application);
                fizzNacosProperties.setIp(ipAddress == null ? applicationContext.getBean(InetUtils.class).findFirstNonLoopbackAddress().getHostAddress() : ipAddress);
                fizzNacosProperties.setPort(Integer.parseInt(port));
                fizzNacosProperties.setNamespace(namespace.equals("") ? null : namespace);
                fizzNacosProperties.setGroup(group);
                fizzNacosProperties.setClusterName(clusterName);
                fizzNacosProperties.setNamespace("");
                fizzNacosProperties.setSecretKey("");
                fizzNacosProperties.setAccessKey("");
                fizzNacosProperties.setUsername("");
                fizzNacosProperties.setPassword("");
                fizzNacosProperties.setEndpoint("");
                fizzNacosProperties.setLogName("");
                fizzNacosProperties.setNamingLoadCacheAtStart("false");
                fizzNacosProperties.setServerAddr(serviceUrl);

                FizzNacosServiceRegistration fizzNacosServiceRegistration = FizzNacosHelper.getServiceRegistration(fizzNacosProperties);
                serviceRegistry = fizzNacosServiceRegistration.serviceRegistry;
                registration    = fizzNacosServiceRegistration.registration;
            }

            serviceRegistry.register(registration);
        }
    }

    @PreDestroy
    public void stop() {
        if (serviceRegistry != null) {
            serviceRegistry.deregister(registration);
        }
    }
}
