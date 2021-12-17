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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import we.Fizz;
import we.service_registry.eureka.FizzEurekaHelper;
import we.service_registry.nacos.FizzNacosHelper;
import we.util.JacksonUtils;
import we.util.YmlUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * @author hongqiaowei
 */

public class RegistryCenter {

    public static final int EUREKA = 1;
    public static final int NACOS  = 2;

    public static final int YML        = 1;
    public static final int PROPERTIES = 2;

    @JsonProperty(
        access = JsonProperty.Access.WRITE_ONLY
    )
    public boolean isDeleted = false;
    public long    id;
    public String  name;
    public int     type;
    public int     clientConfigFormat;
    public String  clientConfig;

    @JsonIgnore
    public FizzServiceRegistration fizzServiceRegistration;

    @JsonCreator
    public RegistryCenter(
                                @JsonProperty("isDeleted") int    isDeleted,
                                @JsonProperty("id")        long   id,
                                @JsonProperty("name")      String name,
                                @JsonProperty("type")      int    type,
                                @JsonProperty("format")    int    clientConfigFormat,
                                @JsonProperty("content")   String clientConfig
    ) throws IOException {

        if (isDeleted == 1) {
            this.isDeleted = true;
        }
        this.id                 = id;
        this.name               = name;
        this.type               = type;
        this.clientConfigFormat = clientConfigFormat;
        this.clientConfig       = clientConfig;

        Properties properties;
        if (this.clientConfigFormat == YML) {
            properties = YmlUtils.string2properties(clientConfig);
        } else {
            Resource resource = new ByteArrayResource(clientConfig.getBytes());
            properties = PropertiesLoaderUtils.loadProperties(resource);
        }
        if (type == EUREKA) {
            fizzServiceRegistration = FizzEurekaHelper.getServiceRegistration(Fizz.context, properties);
        } else {
            fizzServiceRegistration = FizzNacosHelper.getServiceRegistration(Fizz.context, properties);
        }
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
