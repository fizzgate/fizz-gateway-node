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
package we.util;

/**
 * Resource ID utility
 * Extracted from {@link we.stats.ratelimit.ResourceRateLimitConfig}
 *
 * @author zhongjie
 */
public class ResourceIdUtils {

    public  static final String NODE                       = "_global";

    public  static final String NODE_RESOURCE              = buildResourceId(null, null, NODE, null, null);

    public  static final String SERVICE_DEFAULT            = "service_default";

    public  static final String SERVICE_DEFAULT_RESOURCE   = buildResourceId(null, null, null, SERVICE_DEFAULT, null);

    public  static final String APP_DEFAULT                = "app_default";

    public  static final String APP_DEFAULT_RESOURCE       = buildResourceId(APP_DEFAULT, null, null, null, null);

    public static String buildResourceId(String app, String ip, String node, String service, String path) {
        StringBuilder b = new StringBuilder(32);
        buildResourceIdTo(b, app, ip, node, service, path);
        return b.toString();
    }

    public static void buildResourceIdTo(StringBuilder b, String app, String ip, String node, String service, String path) {
        b.append(app     == null ? Consts.S.EMPTY : app)     .append(Consts.S.SQUARE);
        b.append(ip      == null ? Consts.S.EMPTY : ip)      .append(Consts.S.SQUARE);
        b.append(node    == null ? Consts.S.EMPTY : node)    .append(Consts.S.SQUARE);
        b.append(service == null ? Consts.S.EMPTY : service) .append(Consts.S.SQUARE);
        b.append(path    == null ? Consts.S.EMPTY : path);
    }

    public static String getApp(String resource) {
        int i = resource.indexOf(Consts.S.SQUARE);
        if (i == 0) {
            return null;
        } else {
            return resource.substring(0, i);
        }
    }

    public static String getIp(String resource) {
        String extract = Utils.extract(resource, Consts.S.SQUARE, 1);
        if (extract.equals(Consts.S.EMPTY)) {
            return null;
        }
        return extract;
    }

    public static String getNode(String resource) {
        String extract = Utils.extract(resource, Consts.S.SQUARE, 2);
        if (extract.equals(Consts.S.EMPTY)) {
            return null;
        }
        return extract;
    }

    public static String getService(String resource) {
        String extract = Utils.extract(resource, Consts.S.SQUARE, 3);
        if (extract.equals(Consts.S.EMPTY)) {
            return null;
        }
        return extract;
    }

    public static String getPath(String resource) {
        int i = resource.lastIndexOf(Consts.S.SQUARE);
        if (i == resource.length() - 1) {
            return null;
        } else {
            return resource.substring(i);
        }
    }
}
