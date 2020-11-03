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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;

import we.plugin.PluginConfig;
import we.util.Constants;
import we.util.JacksonUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author hongqiaowei
 */

public class ApiConfig {

    public static final int       DELETED = 1;

    public static final char      ALLOW   = 'a';

    public static final char      FORBID  = 'f';

    public static final byte      DIRECT_PROXY_MODE         = 1;

    public static final byte      PREFIX_REWRITE_PROXY_MODE = 2;

    // @JsonIgnore
    public  int                id;                            // tb_api_auth.id

    // @JsonIgnore
    public  int                isDeleted        = 0;          // tb_api_auth.is_deleted

    public  Set<String>        gatewayGroups    = Stream.of(GatewayGroup.DEFAULT).collect(Collectors.toSet());

    public  String             service;

    public  HttpMethod         method           = HttpMethod.X;

    public  String             path             = String.valueOf(Constants.Symbol.FORWARD_SLASH);

    public  Set<String>        apps             = Stream.of(App.ALL_APP).collect(Collectors.toSet());

    public  byte               proxyMode        = DIRECT_PROXY_MODE;

    private AtomicInteger      counter          = new AtomicInteger(-1);

    public  List<String>       backendUrls;

    public  char               access           = ALLOW;

    public  List<PluginConfig> pluginConfigs;

    public void setGatewayGroup(String ggs) {
        gatewayGroups.remove(GatewayGroup.DEFAULT);
        if (StringUtils.isBlank(ggs)) {
            gatewayGroups.add("*");
        } else {
            Arrays.stream(StringUtils.split(ggs, ',')).forEach(
                    gg -> {
                        gatewayGroups.add(gg.trim());
                    }
            );
        }
    }

    public void setApp(String as) {
        apps.remove(App.ALL_APP);
        if (StringUtils.isBlank(as)) {
            apps.add("*");
        } else {
            Arrays.stream(StringUtils.split(as, ',')).forEach(
                    a -> {
                        apps.add(a.trim());
                    }
            );
        }
    }

    public void setPath(String p) {
        if (StringUtils.isNotBlank(p)) {
            path = p.trim();
        }
    }

    public void setMethod(String m) {
        method = HttpMethod.resolve(m);
        if (method == null) {
            method = HttpMethod.X;
        }
    }

    @JsonIgnore
    public String getNextBackendUrl() {
        int idx = counter.incrementAndGet();
        if (idx < 0) {
            counter.set(0);
            idx = 0;
        }
        return backendUrls.get(idx % backendUrls.size());
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
