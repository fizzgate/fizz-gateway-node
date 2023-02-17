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

package com.fizzgate.service_registry.eureka;

import com.fizzgate.util.Consts;
import com.fizzgate.util.PropertiesUtils;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.loadbalancer.support.SimpleObjectProvider;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.InstanceInfoFactory;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author hongqiaowei
 */

public abstract class FizzEurekaHelper {

    private static final int el  = "eureka."           .length();
    private static final int ecl = "eureka.client."    .length();
    private static final int eil = "eureka.instance."  .length();

    private FizzEurekaHelper() {
    }

    public static FizzEurekaServiceRegistration getServiceRegistration(ApplicationContext applicationContext, Properties eurekaProperties) {

        Properties eurekaProps = new Properties();
        /*for (String propertyName : eurekaProperties.stringPropertyNames()) {
            String pn = null;
            if (propertyName.charAt(ecl - 1) == Consts.S.DOT) {
                pn = propertyName.substring(ecl);
            } else if (propertyName.charAt(eil - 1) == Consts.S.DOT) {
                pn = propertyName.substring(eil);
            } else {
                pn = propertyName.substring(el);
            }
            if (pn.indexOf(Consts.S.DASH) > -1) {
                pn = PropertiesUtils.normalize(pn);
            }
            eurekaProps.setProperty(pn, eurekaProperties.getProperty(propertyName));
        }*/

        eurekaProperties.forEach(
                (n, v) -> {
                    String propertyName = (String) n;
                    String pn = null;
                    if (propertyName.charAt(ecl - 1) == Consts.S.DOT) {
                        pn = propertyName.substring(ecl);
                    } else if (propertyName.charAt(eil - 1) == Consts.S.DOT) {
                        pn = propertyName.substring(eil);
                    } else {
                        pn = propertyName.substring(el);
                    }
                    if (pn.indexOf(Consts.S.DASH) > -1) {
                        pn = PropertiesUtils.normalize(pn);
                    }
                    eurekaProps.put(pn, v);
                }
        );

        InetUtils inetUtils = null;
        try {
            inetUtils = applicationContext.getBean(InetUtils.class);
        } catch (NoSuchBeanDefinitionException e) {
            inetUtils = new InetUtils(new InetUtilsProperties());
        }
        EurekaInstanceConfigBean eurekaInstanceConfig = new EurekaInstanceConfigBean(inetUtils);
        PropertiesUtils.setBeanPropertyValue(eurekaInstanceConfig, eurekaProps);

        String appname = eurekaInstanceConfig.getAppname();
        if (appname == null || appname.equals("unknown")) {
            appname = applicationContext.getEnvironment().getProperty("spring.application.name");
            eurekaInstanceConfig.setAppname(appname);
        }

        // VirtualHostName
        String virtualHostName = eurekaInstanceConfig.getVirtualHostName();
        if (virtualHostName == null || virtualHostName.equals("unknown")) {
            eurekaInstanceConfig.setVirtualHostName(appname);
        }

        String serverPort = eurekaProps.getProperty("serverPort");
        if (serverPort == null) {
            serverPort = applicationContext.getEnvironment().getProperty("server.port");
        }
        assert serverPort != null;
        eurekaInstanceConfig.setNonSecurePort(Integer.parseInt(serverPort));

        String ipAddress = System.getProperty("eureka.instance.ip-address");
        if (StringUtils.isBlank(ipAddress)) {
            ipAddress = System.getenv("eureka.instance.ip-address");
            if (StringUtils.isBlank(ipAddress)) {
                ipAddress = eurekaInstanceConfig.getIpAddress();
                if (ipAddress == null) {
                    ipAddress = inetUtils.findFirstNonLoopbackAddress().getHostAddress();
                }
            }
        }
        eurekaInstanceConfig.setIpAddress(ipAddress);

        String instanceId = eurekaInstanceConfig.getInstanceId();
        if (instanceId == null) {
            eurekaInstanceConfig.setInstanceId(ipAddress + ':' + appname + ':' + serverPort);
        }

        InstanceInfo instanceInfo = new InstanceInfoFactory().create(eurekaInstanceConfig);
        ApplicationInfoManager applicationInfoManager = new ApplicationInfoManager(eurekaInstanceConfig, instanceInfo);

        EurekaClientConfigBean eurekaClientConfig = new EurekaClientConfigBean();
        Map<String, Class<?>> propertyTypeHint = new HashMap<>();
        propertyTypeHint.put("serviceUrl", Map.class);
        PropertiesUtils.setBeanPropertyValue(eurekaClientConfig, eurekaProps, propertyTypeHint);

        CloudEurekaClient eurekaClient = new CloudEurekaClient(applicationInfoManager, eurekaClientConfig, null, applicationContext);

        SimpleObjectProvider<HealthCheckHandler> healthCheckHandler = new SimpleObjectProvider<>(null);
        EurekaRegistration eurekaRegistration = EurekaRegistration.builder(eurekaInstanceConfig).with(applicationInfoManager).with(healthCheckHandler).with(eurekaClient).build();
        EurekaServiceRegistry serviceRegistry = new EurekaServiceRegistry();
        String registerCenter = eurekaProps.getProperty("register-center");
        if (registerCenter == null) {
            registerCenter = eurekaClientConfig.getServiceUrl().get(EurekaClientConfigBean.DEFAULT_ZONE);
        }
        return new FizzEurekaServiceRegistration(registerCenter, eurekaRegistration, serviceRegistry, eurekaClient);
    }
}
