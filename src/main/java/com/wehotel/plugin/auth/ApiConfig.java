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

import com.wehotel.plugin.PluginConfig;
import com.wehotel.util.Constants;
import com.wehotel.util.JacksonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * @author lancer
 */

public class ApiConfig {

    public static final int  DELETED = 1;

    public static final char ALLOW   = 'a';

    public static final char FORBID  = 'f';

    public  int                id;                         // tb_api_auth.id

    public  int                isDeleted     = 0;          // tb_api_auth.is_deleted

    public  char               gatewayGroup;               // tb_api_auth.gateway_group

    public  String             service;

    public  HttpMethod         method        = HttpMethod.X;

    public  String             path          = String.valueOf(Constants.Symbol.FORWARD_SLASH);

    private String             app;

    public  char               access        = ALLOW;

    public  List<PluginConfig> pluginConfigs;

    public void setApp(String a) {
        app = a;
    }

    public String app() {
        if (StringUtils.isBlank(app)) {
            if (gatewayGroup == GatewayGroup.C) {
                app = App.TO_C;
            } else if (gatewayGroup == GatewayGroup.B) {
                app = App.TO_B;
            } else {
                throw new RuntimeException(toString() + " no app", null, false, false) {};
            }
        }
        return app;
    }

    public void setPath(String p) {
        if (StringUtils.isNotBlank(p)) {
            path = p;
        }
    }

    public void setMethod(String m) {
        method = HttpMethod.resolve(m);
        if (method == null) {
            method = HttpMethod.X;
        }
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
