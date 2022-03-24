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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import we.util.JacksonUtils;

import static we.util.ResourceIdUtils.APP_DEFAULT;
import static we.util.ResourceIdUtils.NODE;
import static we.util.ResourceIdUtils.SERVICE_DEFAULT;

/**
 * @author hongqiaowei
 */

public class ResourceRateLimitConfig {

    public static interface Type {
        static final byte NODE            = 1;
        static final byte SERVICE_DEFAULT = 2;
        static final byte SERVICE         = 3;
        static final byte API             = 4;
        static final byte APP_DEFAULT     = 5;
        static final byte APP             = 6;
        static final byte IP              = 7;
        static final byte HOST            = 8;
    }

    public  boolean isDeleted = false;

    public  int     id;

    private boolean enable    = true;

    public  String  resource;

    public  String  service;

    public  String  path;

    public  String  app;

    public  String  ip;

    public  String  node;

    public  byte    type;

    public  long    qps;

    public  long    concurrents;

    public  String  responseType;

    public  String  responseContent;

    public boolean isEnable() {
        return enable;
    }

    public void setDeleted(int v) {
        if (v == 1) {
            isDeleted = true;
        }
    }

    public void setEnable(int v) {
        if (v == 1) {
            enable = true;
        } else {
            enable = false;
        }
    }

    public void setResource(String r) {
        if (StringUtils.isNotBlank(r)) {
            resource = r;
            if (!resource.equals(NODE)) {
                service = resource;
            }
        }
    }

    public void setType(byte t) {
        type = t;
        if (type == Type.NODE) {
            node = NODE;
        } else if (type == Type.SERVICE_DEFAULT) {
            service = SERVICE_DEFAULT;
        } else if (type == Type.APP_DEFAULT) {
            app = APP_DEFAULT;
        }
    }

    public void setService(String s) {
        if (StringUtils.isNotBlank(s)) {
            service = s;
        }
    }

    public void setPath(String p) {
        if (StringUtils.isNotBlank(p)) {
            path = p;
        }
    }

    private String resourceId = null;

    @JsonIgnore
    public String getResourceId() {
        if (resourceId == null) {
            resourceId =
                    (app     == null ? "" : app)     + '^' +
                    (ip      == null ? "" : ip)      + '^' +
                    (node    == null ? "" : node)    + '^' +
                    (service == null ? "" : service) + '^' +
                    (path    == null ? "" : path)
            ;
        }
        return resourceId;
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
