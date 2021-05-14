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

package we.plugin.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.util.JacksonUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author hongqiaowei
 */

public class GatewayGroup2apiConfig {

    // private static final Logger log = LoggerFactory.getLogger(GatewayGroup2apiConfig.class);

    // private Map<String/*gg*/, Map<String/*a*/, ApiConfig>> configMap = new HashMap<>(8);
    private Map<String/*gg*/, Set<ApiConfig>> configMap = new HashMap<>(8);

    @JsonProperty(value = "configs", access = JsonProperty.Access.READ_ONLY)
    public Map<String, Set<ApiConfig>> getConfigMap() {
        return configMap;
    }

    // public void setConfigMap(Map<String, Map<String, ApiConfig>> configMap) {
    //     this.configMap = configMap;
    // }

    public void add(ApiConfig ac) {
        for (String gg : ac.gatewayGroups) {
            // Map<String, ApiConfig> app2apiConfigMap = configMap.get(gg);
            // if (app2apiConfigMap == null) {
            //     app2apiConfigMap = new HashMap<>();
            //     configMap.put(gg, app2apiConfigMap);
            // }
            // for (String a : ac.apps) {
            //     app2apiConfigMap.put(a, ac);
            //     log.info("expose " + ac + " to " + gg + " group and " + a + " app");
            // }
            Set<ApiConfig> acs = configMap.get(gg);
            if (acs == null) {
                acs = new HashSet<>(6);
                configMap.put(gg, acs);
            }
            acs.add(ac);
        }
    }

    public void remove(ApiConfig ac) {
        for (String gg : ac.gatewayGroups) {
            // Map<String, ApiConfig> app2apiConfigMap = configMap.get(gg);
            // if (app2apiConfigMap != null) {
            //     for (String a : ac.apps) {
            //         ApiConfig r = app2apiConfigMap.remove(a);
            //         log.info("remove " + r + " from " + gg + " group and " + a + " app");
            //     }
            // }
            Set<ApiConfig> acs = configMap.get(gg);
            if (acs != null) {
                acs.remove(ac);
                if (acs.isEmpty()) {
                    configMap.remove(gg);
                }
            }
        }
    }

    public void update(ApiConfig ac) {
        for (String gg : ac.gatewayGroups) {
            // Map<String, ApiConfig> app2apiConfigMap = configMap.get(gg);
            // if (app2apiConfigMap == null) {
            //     app2apiConfigMap = new HashMap<>();
            //     configMap.put(gg, app2apiConfigMap);
            // }
            // for (String a : ac.apps) {
            //     ApiConfig old = app2apiConfigMap.put(a, ac);
            //     log.info(gg + " group and " + a + " app update " + old + " with " + ac);
            // }
            Set<ApiConfig> acs = configMap.get(gg);
            if (acs == null) {
                acs = new HashSet<>(6);
                configMap.put(gg, acs);
            }
            acs.add(ac);
        }
    }

    // public ApiConfig get(String gatewayGroup, String app) {
    //     Map<String, ApiConfig> app2apiConfigMap = configMap.get(gatewayGroup);
    //     if (app2apiConfigMap == null) {
    //         return null;
    //     } else {
    //         return app2apiConfigMap.get(app);
    //     }
    // }

    public Set<ApiConfig> get(String gatewayGroup) {
        return configMap.get(gatewayGroup);
    }

    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
