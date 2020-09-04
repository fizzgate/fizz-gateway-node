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

import we.util.JacksonUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author lancer
 */

public class App {

    public static final String TO_C             =  "2c";

    public static final String TO_B             =  "2b";

    public static final int    DELETED          =  1;

    public static final int    SIGN_AUTH        =  1;

    public static final int    CUSTOM_AUTH      =  2;

    public int         isDeleted                =  0;        // tb_app_auth.is_deleted

    public int         id;                                   // tb_app_auth.id

    public String      app;                                  // tb_app_auth.app

    public String      name;                                 // tb_app_auth.app_name

    public boolean     useAuth                  =  false;    // 0:false, 1:true

    public int         authType;

    public String      secretkey;

    public boolean     useWhiteList             =  false;

    public Set<String> ips = Collections.emptySet();

    public void setUseAuth(int i) {
        if (i == 1) {
            useAuth = true;
        }
    }

    public void setUseWhiteList(int i) {
        if (i == 1) {
            useWhiteList = true;
        }
    }

    public void setIps(String ips) {
        if (StringUtils.isNotBlank(ips)) {
            this.ips = Arrays.stream(StringUtils.split(ips, ',')).collect(Collectors.toSet());
        }
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
