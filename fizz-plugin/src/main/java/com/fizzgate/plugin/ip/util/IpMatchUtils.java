package com.fizzgate.plugin.ip.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ipv4匹配工具
 */
public final class IpMatchUtils {
    // IP正则
    private static final Pattern IP_PATTERN = Pattern.compile(
            "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\." + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\."
                    + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\." + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})");
    public static final String DEFAULT_ALLOW_ALL_FLAG = "*";// 允许所有ip标志位
    public static final String DEFAULT_DENY_ALL_FLAG = "0"; // 禁止所有ip标志位
    private static final String RANGE_SPLITTER = "-"; // ip范围分隔符

    /**
     * 根据IP白名单设置获取可用的IP列表
     */
    public static Set<String> ipConfigList(String allowIp) {
        // 拆分出白名单正则
        Set<String> set = Sets.newHashSet(Splitter.on(CharMatcher.anyOf(",\n")).trimResults().split(allowIp));
        set = set.stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        return ipConfigList(Sets.newHashSet(set));
    }

    /**
     * 根据IP白名单设置获取可用的IP列表
     */
    public static Set<String> ipConfigList(Set<String> allowIpList) {
        Set<String> ipList = new HashSet<>(allowIpList.size());
        for (String allow : allowIpList) {
            if (allow.contains("*")) { // 处理通配符 *
                String[] ips = allow.split("\\.");
                String[] from = new String[]{"0", "0", "0", "0"};
                String[] end = new String[]{"255", "255", "255", "255"};
                List<String> tem = new ArrayList<>();
                for (int i = 0; i < ips.length; i++)
                    if (ips[i].contains("*")) {
                        tem = complete(ips[i]);
                        from[i] = null;
                        end[i] = null;
                    } else {
                        from[i] = ips[i];
                        end[i] = ips[i];
                    }

                StringBuilder fromIP = new StringBuilder();
                StringBuilder endIP = new StringBuilder();
                for (int i = 0; i < 4; i++) {
                    if (from[i] != null) {
                        fromIP.append(from[i]).append(".");
                        endIP.append(end[i]).append(".");
                    } else {
                        fromIP.append("[*].");
                        endIP.append("[*].");
                    }
                }
                fromIP.deleteCharAt(fromIP.length() - 1);
                endIP.deleteCharAt(endIP.length() - 1);

                for (String s : tem) {
                    String ip = fromIP.toString().replace("[*]", s.split(";")[0]) + RANGE_SPLITTER
                            + endIP.toString().replace("[*]", s.split(";")[1]);
                    if (validate(ip)) {
                        ipList.add(ip);
                    }
                }
            } else if (allow.contains("/")) { // 处理 网段 xxx.xxx.xxx./24
                ipList.add(allow);
            } else { // 处理单个 ip 或者 范围
                if (validate(allow)) {
                    ipList.add(allow);
                }
            }

        }

        return ipList;
    }

    /**
     * 对单个IP节点进行范围限定
     *
     * @param arg
     * @return 返回限定后的IP范围，格式为List[10;19, 100;199]
     */
    private static List<String> complete(String arg) {
        List<String> com = new ArrayList<>();
        int len = arg.length();
        if (len == 1) {
            com.add("0;255");
        } else if (len == 2) {
            String s1 = complete(arg, 1);
            if (s1 != null) {
                com.add(s1);
            }
            String s2 = complete(arg, 2);
            if (s2 != null) {
                com.add(s2);
            }
        } else {
            String s1 = complete(arg, 1);
            if (s1 != null) {
                com.add(s1);
            }
        }
        return com;
    }

    private static String complete(String arg, int length) {
        String from = "";
        String end = "";
        if (length == 1) {
            from = arg.replace("*", "0");
            end = arg.replace("*", "9");
        } else {
            from = arg.replace("*", "00");
            end = arg.replace("*", "99");
        }
        if (Integer.parseInt(from) > 255) {
            return null;
        }
        if (Integer.parseInt(end) > 255) {
            end = "255";
        }
        return from + ";" + end;
    }

    /**
     * 在添加至白名单时进行格式校验
     */
    private static boolean validate(String ip) {
        String[] temp = ip.split(RANGE_SPLITTER);
        for (String s : temp) {
            if (!IP_PATTERN.matcher(s).matches()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据IP,及可用Ip列表来判断ip是否包含在白名单之中
     */
    public static boolean match(String ip, Set<String> ipList) {
        if (CollectionUtils.isEmpty(ipList)) {
            return false;
        }
        if (ipList.contains(ip)) {
            return true;
        }
        IPAddress ipAddress = null;
        try {
            ipAddress = new IPAddressString(ip).toAddress();
        } catch (AddressStringException e) {
            throw new RuntimeException(e);
        }
        for (String allow : ipList) {
            if (allow.contains(RANGE_SPLITTER)) { // 处理 类似 192.168.0.0-192.168.2.1
                String[] tempAllow = allow.split(RANGE_SPLITTER);
                String[] from = tempAllow[0].split("\\.");
                String[] end = tempAllow[1].split("\\.");
                String[] tag = ip.split("\\.");
                boolean check = true;
                for (int i = 0; i < 4; i++) { // 对IP从左到右进行逐段匹配
                    int s = Integer.parseInt(from[i]);
                    int t = Integer.parseInt(tag[i]);
                    int e = Integer.parseInt(end[i]);
                    if (!(s <= t && t <= e)) {
                        check = false;
                        break;
                    }
                }
                if (check) {
                    return true;
                }
            } else if (allow.contains("/")) { // 处理 网段 xxx.xxx.xxx./24
                int splitIndex = allow.indexOf("/");
                // 取出子网段
                String ipSegment = allow.substring(0, splitIndex); // 192.168.3.0
                // 子网数
                String netmask = allow.substring(splitIndex + 1); // 24
                // ip 转二进制
                long ipLong = ipToLong(ip);
                // 子网二进制
                long maskLong = (2L << 32 - 1) - (2L << (32 - Integer.parseInt(netmask)) - 1);
                // ip与和子网相与 得到 网络地址
                String calcSegment = longToIP(ipLong & maskLong);
                // 如果计算得出网络地址和库中网络地址相同 则合法
                if (ipSegment.equals(calcSegment)) {
                    return true;
                }
            } else if (allow.contains("*")) {
                IPAddress rangeAddress = new IPAddressString(allow).getAddress();
                if (rangeAddress.contains(ipAddress)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据IP地址，及IP白名单设置规则判断IP是否包含在白名单
     */
    public static boolean match(String ip, String ipWhiteConfig) {
        if (null == ip || "".equals(ip)) {
            return false;
        }
        // ip格式不对
        if (!IP_PATTERN.matcher(ip).matches()) {
            return false;
        }
        if (DEFAULT_ALLOW_ALL_FLAG.equals(ipWhiteConfig)) {
            return true;
        }
        if (DEFAULT_DENY_ALL_FLAG.equals(ipWhiteConfig)) {
            return false;
        }
        Set<String> ipList = ipConfigList(ipWhiteConfig);
        return match(ip, ipList);
    }

    /**
     * 将 127.0.0.1形式的IP地址 转换成 10进制整数形式
     */
    private static long ipToLong(String strIP) {
        long[] ip = new long[4];
        // 先找到IP地址字符串中.的位置
        int position1 = strIP.indexOf(".");
        int position2 = strIP.indexOf(".", position1 + 1);
        int position3 = strIP.indexOf(".", position2 + 1);
        // 将每个.之间的字符串转换成整型
        ip[0] = Long.parseLong(strIP.substring(0, position1));
        ip[1] = Long.parseLong(strIP.substring(position1 + 1, position2));
        ip[2] = Long.parseLong(strIP.substring(position2 + 1, position3));
        ip[3] = Long.parseLong(strIP.substring(position3 + 1));
        return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
    }

    /**
     * 将 10进制整数形式 转换成 127.0.0.1形式的IP地址
     */
    private static String longToIP(long longIP) {
        // 直接右移24位
        return "" + (longIP >>> 24) +
                "." +
                // 将高8位置0，然后右移16位
                ((longIP & 0x00FFFFFF) >>> 16) +
                "." +
                ((longIP & 0x0000FFFF) >>> 8) +
                "." +
                (longIP & 0x000000FF);
    }

}