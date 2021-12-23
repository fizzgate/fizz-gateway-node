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
import org.apache.commons.lang3.StringUtils;
import we.util.Consts;
import we.util.JacksonUtils;

/**
 * @author hongqiaowei
 */

public class Receiver {

    public String service;

    public String registryCenter;

    public int    type;

    public String path;

    @JsonProperty("registryName")
    public void setRegistryCenter(String rc) {
        if (StringUtils.isNotBlank(rc)) {
            if (rc.equals(Consts.S.DEFAULT)) {
                registryCenter = Consts.S.DEFAULT;
            } else {
                registryCenter = rc;
            }
        }
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
