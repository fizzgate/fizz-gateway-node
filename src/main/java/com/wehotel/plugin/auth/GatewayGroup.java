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
package com.wehotel.plugin.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lancer
 */

public class GatewayGroup {

    private static final Logger log = LoggerFactory.getLogger(GatewayGroup.class);

    public static final char C = 'c';

    public static final char B = 'b';

    public static final char T = 't';

    public char id;

    private Map<String, ServiceConfig> serviceConfigMap = new HashMap<>(128);

    public GatewayGroup(char id) {
        this.id = id;
    }

    public Map<String, ServiceConfig> getServiceConfigMap() {
        return serviceConfigMap;
    }

    @JsonIgnore
    public ServiceConfig getServiceConfig(String id) {
        return serviceConfigMap.get(id);
    }

    public void remove(ApiConfig ac) {
        ServiceConfig sc = serviceConfigMap.get(ac.service);
        if (sc == null) {
            log.info("no service config for " + ac);
        } else {
            sc.remove(ac);
            if (sc.apiConfigMap().isEmpty()) {
                serviceConfigMap.remove(ac.service);
            }
        }
    }

    public void add(ApiConfig ac) {
        ServiceConfig sc = new ServiceConfig(ac.service);
        serviceConfigMap.put(ac.service, sc);
        sc.add(ac);
    }

    public void update(ApiConfig ac) {
        ServiceConfig sc = serviceConfigMap.get(ac.service);
        if (sc == null) {
            add(ac);
        } else {
            sc.update(ac);
        }
    }
}
