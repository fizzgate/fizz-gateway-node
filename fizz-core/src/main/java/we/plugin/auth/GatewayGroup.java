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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.plugin.PluginConfig;
import we.util.JacksonUtils;

import java.util.*;

/**
 * @author hongqiaowei
 */

public class GatewayGroup {

    private static final Logger log = LoggerFactory.getLogger(GatewayGroup.class);

    public  static final String DEFAULT = "default";

    public  static final int    DELETED = 1;

    public int                 id;

    public int                 isDeleted     = 0;

    public String              group;

    public String              name;

    public Set<String>         gateways      = new HashSet<>();

    public List<PluginConfig>  pluginConfigs = Collections.emptyList();

    public void setGateways(String gateways) {
        if (StringUtils.isNotBlank(gateways)) {
            Arrays.stream(StringUtils.split(gateways, ',')).forEach(
                    gw -> {
                        this.gateways.add(gw.trim());
                    }
            );
        }
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
