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

package we.service_registry.nacos;

import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.naming.NamingService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import we.util.Consts;
import we.util.JacksonUtils;
import we.util.PropertiesUtils;
import we.util.ReflectionUtils;

import java.util.Properties;

/**
 * @author hongqiaowei
 */

public abstract class FizzNacosHelper {

    private static final int ndl = "nacos.discovery.".length();

    private FizzNacosHelper() {
    }

    public static FizzNacosServiceRegistration getServiceRegistration(ApplicationContext applicationContext, Properties nacosProperties) {

        Properties ps = new Properties();
        for (String propertyName : nacosProperties.stringPropertyNames()) {
            String propertyValue = nacosProperties.getProperty(propertyName);
            if (propertyName.endsWith(PropertyKeyConst.USERNAME)) {
                ps.setProperty(PropertyKeyConst.USERNAME, propertyValue);
            } else if (propertyName.endsWith(PropertyKeyConst.PASSWORD)) {
                ps.setProperty(PropertyKeyConst.PASSWORD, propertyValue);
            } else {
                String pn = propertyName.substring(ndl);
                if (pn.indexOf(Consts.S.DASH) > -1) {
                    pn = PropertiesUtils.normalize(pn);
                }
                ps.setProperty(pn, propertyValue);
            }
        }

        FizzNacosProperties fizzNacosProperties = new FizzNacosProperties(ps);
        PropertiesUtils.setBeanPropertyValue(fizzNacosProperties, ps);

        fizzNacosProperties.setApplicationContext(applicationContext);
        if (fizzNacosProperties.getId() == null) {
            fizzNacosProperties.setId(fizzNacosProperties.getServerAddr());
        }
        Environment env = applicationContext.getEnvironment();
        if (fizzNacosProperties.getService() == null) {
            fizzNacosProperties.setService(env.getProperty("spring.application.name"));
        }
        String ip = fizzNacosProperties.getIp();
        if (StringUtils.isBlank(ip)) {
            ip = System.getProperty("nacos.discovery.ip");
            if (StringUtils.isBlank(ip)) {
                ip = System.getenv("nacos.discovery.ip");
            }
            if (StringUtils.isNotBlank(ip)) {
                fizzNacosProperties.setIp(ip);
            }
        }
        if (fizzNacosProperties.getPort() == -1) {
            fizzNacosProperties.setPort(Integer.parseInt(env.getProperty("server.port")));
        }
        fizzNacosProperties.setNamingLoadCacheAtStart("false");

        fizzNacosProperties.init();

        NacosServiceRegistry nacosServiceRegistry = new NacosServiceRegistry(fizzNacosProperties);
        NacosServiceManager nacosServiceManager = new NacosServiceManager();
        ReflectionUtils.set(nacosServiceRegistry, "nacosServiceManager", nacosServiceManager);
        NacosRegistration nacosRegistration = new NacosRegistration(null, fizzNacosProperties, applicationContext);
        Properties nps = fizzNacosProperties.getNacosProperties();
        NamingService namingService = nacosServiceManager.getNamingService(nps);
        return new FizzNacosServiceRegistration(fizzNacosProperties.getId(), nacosRegistration, nacosServiceRegistry, namingService);
    }
}
