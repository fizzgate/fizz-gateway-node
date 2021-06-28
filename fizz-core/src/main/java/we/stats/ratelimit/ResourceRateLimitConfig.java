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
import we.util.Constants;
import we.util.JacksonUtils;
import we.util.Utils;

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
    }

    public  static final int    DELETED                    = 1;

    public  static final String NODE                       = "_global";

    public  static final String NODE_RESOURCE              = buildResourceId(null, null, NODE, null, null);

    public  static final String SERVICE_DEFAULT            = "service_default";

    public  static final String SERVICE_DEFAULT_RESOURCE   = buildResourceId(null, null, null, SERVICE_DEFAULT, null);

    public  static final String APP_DEFAULT                = "app_default";

    public  static final String APP_DEFAULT_RESOURCE       = buildResourceId(APP_DEFAULT, null, null, null, null);

    private static final int    ENABLE                     = 1;

    private static final int    UNABLE                     = 0;

    public  int     isDeleted = 0;

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

    public void setEnable(int v) {
        if (v == ENABLE) {
            enable = true;
        } else {
            enable = false;
        }
    }

    public void setResource(String r) {
        resource = r;
        if (!resource.equals(NODE)) {
            service = resource;
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

    public static String buildResourceId(String app, String ip, String node, String service, String path) {
        StringBuilder b = new StringBuilder(32);
        buildResourceIdTo(b, app, ip, node, service, path);
        return b.toString();
    }

    public static void buildResourceIdTo(StringBuilder b, String app, String ip, String node, String service, String path) {
        b.append(app     == null ? Constants.Symbol.EMPTY : app)     .append(Constants.Symbol.SQUARE);
        b.append(ip      == null ? Constants.Symbol.EMPTY : ip)      .append(Constants.Symbol.SQUARE);
        b.append(node    == null ? Constants.Symbol.EMPTY : node)    .append(Constants.Symbol.SQUARE);
        b.append(service == null ? Constants.Symbol.EMPTY : service) .append(Constants.Symbol.SQUARE);
        b.append(path    == null ? Constants.Symbol.EMPTY : path);
    }

    public static String getApp(String resource) {
        int i = resource.indexOf(Constants.Symbol.SQUARE);
        if (i == 0) {
            return null;
        } else {
            return resource.substring(0, i);
        }
    }

    public static String getIp(String resource) {
        String extract = Utils.extract(resource, Constants.Symbol.SQUARE, 1);
        if (extract.equals(Constants.Symbol.EMPTY)) {
            return null;
        }
        return extract;
    }

    public static String getNode(String resource) {
        String extract = Utils.extract(resource, Constants.Symbol.SQUARE, 2);
        if (extract.equals(Constants.Symbol.EMPTY)) {
            return null;
        }
        return extract;
    }

    public static String getService(String resource) {
        String extract = Utils.extract(resource, Constants.Symbol.SQUARE, 3);
        if (extract.equals(Constants.Symbol.EMPTY)) {
            return null;
        }
        return extract;
    }

    public static String getPath(String resource) {
        int i = resource.lastIndexOf(Constants.Symbol.SQUARE);
        if (i == resource.length() - 1) {
            return null;
        } else {
            return resource.substring(i);
        }
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
