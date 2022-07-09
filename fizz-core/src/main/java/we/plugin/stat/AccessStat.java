/*
 *  Copyright (C) 2021 the original author or authors.
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

package we.plugin.stat;

import we.util.JacksonUtils;

/**
 * @author hongqiaowei
 */

public class AccessStat {

    public String service;
    public String apiMethod;
    public String apiPath;
    public long   start;
    public int    reqs = 0;
    public long   reqTime;

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
