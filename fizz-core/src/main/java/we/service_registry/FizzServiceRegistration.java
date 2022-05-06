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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.ApplicationContext;
import we.service_registry.eureka.FizzEurekaHelper;
import we.service_registry.nacos.FizzNacosHelper;
import we.util.PropertiesUtils;
import we.util.YmlUtils;

import java.util.List;
import java.util.Properties;

/**
 * @author hongqiaowei
 */

public abstract class FizzServiceRegistration {

    protected static final Logger LOGGER = LoggerFactory.getLogger(FizzServiceRegistration.class);

    public enum Type {
        EUREKA, NACOS;
    }

    public enum ConfigFormat {
        YML, PROPERTIES;
    }

    public enum ServerStatus {
        UP, DOWN, STARTING, OUT_OF_SERVICE, UNKNOWN;
    }

    protected String          id;

    private   Type            type;

    private   Registration    registration;

    private   ServiceRegistry serviceRegistry;

    public static FizzServiceRegistration getFizzServiceRegistration(ApplicationContext applicationContext, Type type, ConfigFormat configFormat, String config) {
        Properties configProperties;
        if (configFormat == ConfigFormat.YML) {
            configProperties = YmlUtils.string2properties(config);
        } else {
            configProperties = PropertiesUtils.from(config);
        }
        FizzServiceRegistration fizzServiceRegistration;
        if (type == Type.EUREKA) {
            fizzServiceRegistration = FizzEurekaHelper.getServiceRegistration(applicationContext, configProperties);
        } else {
            fizzServiceRegistration = FizzNacosHelper. getServiceRegistration(applicationContext, configProperties);
        }
        return fizzServiceRegistration;
    }

    public FizzServiceRegistration(String id, Type type, Registration registration, ServiceRegistry serviceRegistry) {
        this.id              = id;
        this.type            = type;
        this.registration    = registration;
        this.serviceRegistry = serviceRegistry;
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public void register() {
        serviceRegistry.register(registration);
        LOGGER.info("register to {} {}", type, id);
    }

    public void deregister() {
        serviceRegistry.deregister(registration);
        LOGGER.info("deregister to {} {}", type, id);
    }

    public void close() {
        serviceRegistry.close();
        shutdownClient();
        LOGGER.info("close {} {}", type, id);
    }

    protected abstract void shutdownClient();

    public abstract ServerStatus          getServerStatus();

    public abstract List<String>          getServices();

    public abstract String                getInstance(String service);
}
