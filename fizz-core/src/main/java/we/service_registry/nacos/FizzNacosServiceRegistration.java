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

import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import com.alibaba.nacos.api.naming.NamingService;

/**
 * @author hongqiaowei
 */

public class FizzNacosServiceRegistration {

    public String id;

    public NacosRegistration registration;

    public NacosServiceRegistry serviceRegistry;

    public NamingService namingService;

    public FizzNacosServiceRegistration(String id, NacosRegistration registration, NacosServiceRegistry serviceRegistry, NamingService namingService) {
        this.id = id;
        this.registration = registration;
        this.serviceRegistry = serviceRegistry;
        this.namingService = namingService;
    }
}
