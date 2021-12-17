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

package we.service_registry;

import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import we.proxy.ServiceInstance;

/**
 * @author hongqiaowei
 */

public abstract class FizzServiceRegistration {

    protected String          id;

    private   Registration    registration;

    private   ServiceRegistry serviceRegistry;

    public FizzServiceRegistration(String id, Registration registration, ServiceRegistry serviceRegistry) {
        this.id              = id;
        this.registration    = registration;
        this.serviceRegistry = serviceRegistry;
    }

    public void register() {
        serviceRegistry.register(registration);
    }

    public void deregister() {
        serviceRegistry.deregister(registration);
    }

    public abstract String getInstance(String service);
}
