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
import com.alibaba.nacos.api.naming.NamingService;
import we.util.ReflectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hongqiaowei
 */

public abstract class FizzNacosHelper {

    public static FizzNacosServiceRegistration getServiceRegistration(FizzNacosProperties fizzNacosProperties) {
        fizzNacosProperties.init();
        NacosServiceRegistry nacosServiceRegistry = new NacosServiceRegistry(fizzNacosProperties);
        NacosServiceManager nacosServiceManager = new NacosServiceManager();
        ReflectionUtils.set(nacosServiceRegistry, "nacosServiceManager", nacosServiceManager);
        NacosRegistration nacosRegistration = new NacosRegistration(null, fizzNacosProperties, fizzNacosProperties.getApplicationContext());
        NamingService namingService = nacosServiceManager.getNamingService(fizzNacosProperties.getNacosProperties());
        return new FizzNacosServiceRegistration(fizzNacosProperties.getId(), nacosRegistration, nacosServiceRegistry, namingService);
    }

    public static Map<String, FizzNacosServiceRegistration> getServiceRegistration(List<FizzNacosProperties> fizzNacosPropertiesList) {
        Map<String, FizzNacosServiceRegistration> result = new HashMap<>();
        for (FizzNacosProperties properties : fizzNacosPropertiesList) {
            FizzNacosServiceRegistration fizzNacosServiceRegistration = getServiceRegistration(properties);
            result.put(properties.getId(), fizzNacosServiceRegistration);
        }
        return result;
    }
}
