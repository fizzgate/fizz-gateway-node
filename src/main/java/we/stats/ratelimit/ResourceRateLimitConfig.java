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

package we.stats.ratelimit;

import we.util.JacksonUtils;

/**
 * @author hongqiaowei
 */

public class ResourceRateLimitConfig {

    public static interface Type {
        static final byte GLOBAL          = 1;
        static final byte SERVICE_DEFAULT = 2;
        static final byte SERVICE         = 3;
        static final byte API             = 4;
    }

    public  static final int    DELETED         = 1;

    public  static final String GLOBAL          = "_global";

    public  static final String SERVICE_DEFAULT = "service_default";

    private static final int    ENABLE          = 1;

    private static final int    UNABLE          = 0;

    public  int     isDeleted = 0;

    public  int     id;

    private boolean enable = true;

    public  String  resource;

    public  byte    type;

    public  long    qps;

    public  long    concurrents;

    public  String  responseType;

    public  String  responseContent;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(int v) {
        if (v == ENABLE) {
            enable = true;
        } else {
            enable = false;
        }
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
