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

@Deprecated
public class GatewayGroup2apiConfig {

    private Map<String/*gg*/, Set<ApiConfig>> configMap = new HashMap<>(8);

    @JsonProperty(value = "configs", access = JsonProperty.Access.READ_ONLY)
    public Map<String, Set<ApiConfig>> getConfigMap() {
        return configMap;
    }

    public void add(ApiConfig ac) {
        for (String gg : ac.gatewayGroups) {
            Set<ApiConfig> acs = configMap.get(gg);
            if (acs == null) {
                acs = new HashSet<>(8);
                configMap.put(gg, acs);
            }
            acs.add(ac);
        }
    }

    public void remove(ApiConfig ac) {
        for (String gg : ac.gatewayGroups) {
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
            Set<ApiConfig> acs = configMap.get(gg);
            if (acs == null) {
                acs = new HashSet<>(8);
                configMap.put(gg, acs);
            }
            acs.add(ac);
        }
    }

    public Set<ApiConfig> get(String gatewayGroup) {
        return configMap.get(gatewayGroup);
    }

    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
