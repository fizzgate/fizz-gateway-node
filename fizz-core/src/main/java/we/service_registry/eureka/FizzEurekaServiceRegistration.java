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

package we.service_registry.eureka;

import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;

/**
 * @author hongqiaowei
 */

public class FizzEurekaServiceRegistration {

    public String                id;

    public EurekaRegistration    registration;

    public EurekaServiceRegistry serviceRegistry;

    public CloudEurekaClient     client;

    public FizzEurekaServiceRegistration(String id, EurekaRegistration registration, EurekaServiceRegistry serviceRegistry, CloudEurekaClient client) {
        this.id              = id;
        this.registration    = registration;
        this.serviceRegistry = serviceRegistry;
        this.client          = client;
    }
}
