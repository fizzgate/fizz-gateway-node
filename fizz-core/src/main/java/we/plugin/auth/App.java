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

import org.apache.commons.lang3.StringUtils;

import we.util.Constants;
import we.util.JacksonUtils;

import java.util.*;

/**
 * @author hongqiaowei
 */

public class App {

    public static final String ALL_APP          = "*";

    public static final int    DELETED          =  1;

    static interface AUTH_TYPE {
        static final int SIGN      = 1;
        static final int CUSTOM    = 2;
        static final int SECRETKEY = 3;
    }

    public int                         isDeleted                =  0;        // tb_app_auth.is_deleted

    public int                         id;                                   // tb_app_auth.id

    public String                      app;                                  // tb_app_auth.app

    public String                      name;                                 // tb_app_auth.app_name

    public boolean                     useAuth                  =  false;    // 0:false, 1:true

    public int                         authType;

    public String                      secretkey;

    public boolean                     useWhiteList             =  false;

    public String                      config;

    public Map<String, List<String[]>> ips                      =  new HashMap<>();

    public void setUseAuth(int i) {
        if (i == AUTH_TYPE.SIGN || i == AUTH_TYPE.SECRETKEY || i == AUTH_TYPE.CUSTOM) {
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
            Arrays.stream(StringUtils.split(ips, ',')).forEach(
                    ip -> {
                        ip = ip.trim();
                        int i = ip.lastIndexOf('.');
                        String subnet = ip.substring(0, i).trim();
                        String addrSeg = ip.substring(i + 1).trim();
                        if ("*".equals(addrSeg)) {
                            this.ips.put(subnet, Collections.singletonList(new String[]{"2", "254"}));
                        } else if (addrSeg.indexOf('-') > 0) {
                            String[] a = StringUtils.split(addrSeg, '-');
                            String beg = a[0].trim();
                            String end = a[1].trim();
                            List<String[]> lst = this.ips.get(subnet);
                            if (lst == null) {
                                lst = new ArrayList<>();
                                this.ips.put(subnet, lst);
                            }
                            lst.add(new String[]{beg, end});
                        } else {
                            this.ips.put(ip, null);
                        }
                    }
            );
        }
    }

    public boolean allow(String ip) {
        if (ips.containsKey(ip)) {
            return true;
        }
        int originSubnetLen = ip.lastIndexOf(Constants.Symbol.DOT);
        for (Map.Entry<String, List<String[]>> e : ips.entrySet()) {
            String subnet = e.getKey();
            int subnetLen = subnet.length();
            byte i = 0;
            if (subnetLen == originSubnetLen) {
                for (; i < subnetLen; i++) {
                    if (subnet.charAt(i) != ip.charAt(i)) {
                        break;
                    }
                }
                if (i == subnetLen) {
                    int originAddrLen = ip.length() - originSubnetLen - 1;
                    boolean in = false;
                    for (String[] addrSeg : e.getValue()) {
                        in = inAddrSeg(ip, originSubnetLen, originAddrLen, addrSeg);
                        if (in) {
                            return in;
                        }
                    }
                    return in;
                }
            }
        }
        return false;
    }

    private boolean inAddrSeg(String ip, int originSubnetLen, int originAddrLen, String[] addrSeg) {
        String addrSegBeg = addrSeg[0];
        String addrSegEnd = addrSeg[1];
        if (originAddrLen < addrSegBeg.length() || addrSegEnd.length() < originAddrLen) {
            return false;
        } else {
            boolean b = true;
            if (originAddrLen == addrSegBeg.length()) {
                for (byte j = 0; j < addrSegBeg.length(); j++) {
                    char o = ip.charAt(originSubnetLen + 1 + j);
                    char a = addrSegBeg.charAt(j);
                    if (o < a) {
                        b = false;
                        break;
                    } else if (o > a) {
                        break;
                    }
                }
            }
            if (b) {
                if (originAddrLen == addrSegEnd.length()) {
                    for (byte j = 0; j < addrSegEnd.length(); j++) {
                        char a = addrSegEnd.charAt(j);
                        char o = ip.charAt(originSubnetLen + 1 + j);
                        if (a < o) {
                            b = false;
                            break;
                        } else if (a > o) {
                            break;
                        }
                    }
                }
            }
            return b;
        }
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
