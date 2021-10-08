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

package we.plugin;

import org.apache.commons.lang3.StringUtils;
import we.util.JacksonUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

public class PluginConfig {

    public static final String CUSTOM_CONFIG = "fcK";

    public String plugin; // tb_plugin.eng_name

    public String fixedConfig;

    public Map<String/*tb_api_plugin_config.item*/, Object/*tb_api_plugin_config.value*/> config = Collections.emptyMap();

    // @JsonProperty(value = "config", access = JsonProperty.Access.WRITE_ONLY)
    public void setConfig(String confJson) {
        if (StringUtils.isNotBlank(confJson)) {
            Map m = JacksonUtils.readValue(confJson, Map.class);
            if (config == Collections.EMPTY_MAP) {
                config = m;
            } else {
                config.putAll(m);
            }
        }
    }

    public void setFixedConfig(String fixedConfig) {
        if (StringUtils.isNotBlank(fixedConfig)) {
            if (config == Collections.EMPTY_MAP) {
                config = new HashMap<>();
            }
            config.put(CUSTOM_CONFIG, fixedConfig);
        }
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
