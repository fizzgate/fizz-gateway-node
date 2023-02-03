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

package com.fizzgate.dedicated_line;

import org.springframework.http.HttpMethod;

import com.fizzgate.plugin.auth.ApiConfig;
import com.fizzgate.util.JacksonUtils;

/**
 * @author hongqiaowei
 */

public class MethodAndPath {

    public Object method;

    public String path;

    public void setMethod(String m) {
        method = HttpMethod.resolve(m);
        if (method == null) {
            method = ApiConfig.ALL_METHOD;
        }
    }

    public boolean methodMatch(HttpMethod m) {
        if (method == ApiConfig.ALL_METHOD) {
            return true;
        }
        return method.equals(m);
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
