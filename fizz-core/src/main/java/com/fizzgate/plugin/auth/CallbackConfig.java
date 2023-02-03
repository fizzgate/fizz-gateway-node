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

package com.fizzgate.plugin.auth;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fizzgate.util.JacksonUtils;

/**
 * @author hongqiaowei
 */

public class CallbackConfig {

    public static interface Type {
        static final char ASYNC = 'a';
        static final char SYNC  = 's';
    }

    public int id;

    public char type;

    public List<Receiver> receivers;

    public Map<String, List<String>> respHeaders = Collections.emptyMap();

    public String respBody;

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
