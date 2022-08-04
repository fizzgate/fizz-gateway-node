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
import org.springframework.context.ApplicationContext;
import we.util.JacksonUtils;

import java.util.Objects;

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

    private FizzServiceRegistration fizzServiceRegistration;

    @JsonCreator
    public RegistryCenter(
                                @JsonProperty("isDeleted") int    isDeleted,
                                @JsonProperty("id")        long   id,
                                @JsonProperty("name")      String name,
                                @JsonProperty("type")      int    type,
                                @JsonProperty("format")    int    clientConfigFormat,
                                @JsonProperty("content")   String clientConfig
    ) {

        if (isDeleted == 1) {
            this.isDeleted = true;
        }
        this.id                 = id;
        this.name               = name;
        this.type               = type;
        this.clientConfigFormat = clientConfigFormat;
        this.clientConfig       = clientConfig;
    }

    public void initFizzServiceRegistration(ApplicationContext applicationContext) {
        fizzServiceRegistration = FizzServiceRegistration.getFizzServiceRegistration(
                                          applicationContext,
                                          type               == EUREKA ? FizzServiceRegistration.Type.EUREKA      : FizzServiceRegistration.Type.NACOS,
                                          clientConfigFormat == YML    ? FizzServiceRegistration.ConfigFormat.YML : FizzServiceRegistration.ConfigFormat.PROPERTIES,
                                          clientConfig
                                  );
    }

    @JsonIgnore
    public FizzServiceRegistration getFizzServiceRegistration() {
        return fizzServiceRegistration;
    }

    @JsonIgnore
    public String getInstance(String service) {
        return fizzServiceRegistration.getInstance(service);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegistryCenter that = (RegistryCenter) o;
        return id == that.id && type == that.type && clientConfigFormat == that.clientConfigFormat && Objects.equals(name, that.name) && Objects.equals(clientConfig, that.clientConfig);
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
