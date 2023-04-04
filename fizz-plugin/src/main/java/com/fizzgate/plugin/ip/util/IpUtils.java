package com.fizzgate.plugin.ip.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author huahua
 */
public class IpUtils {

    /*
     * 取所有IP段的私有IP段
     * A类  私有地址  10.0.0.0---10.255.255.255  保留地址 127.0.0.0---127.255.255.255
     * B类  私有地址 172.16.0.0-172.31.255.255
     * C类  私有地址 192.168.0.0-192.168.255.255
     * D类  地址不分网络地址和主机地址
     * E类  地址不分网络地址和主机地址
     */
    private static long
            aBegin = ipToLong("10.0.0.0"),
            aEnd = ipToLong("10.255.255.255"),
            bBegin = ipToLong("172.16.0.0"),
            bEnd = ipToLong("172.31.255.255"),
            cBegin = ipToLong("192.168.0.0"),
            cEnd = ipToLong("192.168.255.255"),
            saveBegin = ipToLong("127.0.0.0"),
            saveEnd = ipToLong("127.255.255.255");

    // 跟IP有关需要做判断的header参数
    private static String
            CLIENT_IP = "clientip",
            X_FORWARDED_FOR = "x-forwarded-for",
            PROXY_CLIENT_IP = "proxy-client-ip",
            WL_PROXY_CLIENT_IP = "wl-proxy-client-ip";
    private static Set<String> ipHeaderNames = Sets.newHashSet(CLIENT_IP, X_FORWARDED_FOR, PROXY_CLIENT_IP, WL_PROXY_CLIENT_IP);

    public static String getServerHttpRequestIp(ServerHttpRequest request) throws SocketException {
        // 防止在header中的参数名有大小写之分，重新将需要处理的参数值和内容装填入Map中
        Map<String, List<String>> ipHeaders = Maps.newHashMap();
        Set<Map.Entry<String, List<String>>> headers = request.getHeaders().entrySet();
        for (Map.Entry<String, List<String>> header : headers) {
            String name = header.getKey();
            String lowerCaseName = name.toLowerCase();
            if (ipHeaderNames.contains(lowerCaseName)) {
                List<String> values = Lists.newArrayList();
                for (String headerValue : header.getValue()) {
                    if (StringUtils.indexOf(headerValue, ",") >= 0) {
                        String[] headerValueArr = StringUtils.split(headerValue, ",");
                        if (headerValueArr.length > 0) {
                            for (String s : headerValueArr) {
                                values.add(StringUtils.trimToEmpty(s));
                            }
                        }
                    } else {
                        values.add(headerValue);
                    }
                }
                ipHeaders.put(lowerCaseName, values); // 装填key和value
            }
        }
        //取正确的IP
        String ipAddress = null;
        // 取clientip
        List<String> clientIpList = ipHeaders.get(CLIENT_IP); // 取clientip与client-ip有区别
        ipAddress = fetchPublicIp(clientIpList);
        // 若clientip为空或者是内网IP则取x-forwarded-for
        if (StringUtils.isBlank(ipAddress)) {
            List<String> xForwardedIpList = ipHeaders.get(X_FORWARDED_FOR);
            ipAddress = fetchPublicIp(xForwardedIpList);
        }
        // 若x-forwarded-for为空则取proxy-client-ip
        if (StringUtils.isBlank(ipAddress)) {
            List<String> proxyClientIpList = ipHeaders.get(PROXY_CLIENT_IP);
            ipAddress = fetchPublicIp(proxyClientIpList);
        }
        // 若proxy-client-ip为空则取wl-proxy-client-ip
        if (StringUtils.isBlank(ipAddress)) {
            List<String> wlProxyClientIpList = ipHeaders.get(WL_PROXY_CLIENT_IP);
            ipAddress = fetchPublicIp(wlProxyClientIpList);
        }
        // 若wl-proxy-client-ip为空则取RemoteAddr
        if (StringUtils.isBlank(ipAddress)) {
            InetSocketAddress inetSocketAddress = request.getRemoteAddress();
            if (inetSocketAddress != null) {
                InetAddress inetAddress = inetSocketAddress.getAddress();
                if (inetAddress instanceof Inet4Address) {
                    ipAddress = inetAddress.getHostAddress();
                }
            }
            if (StringUtils.equals(ipAddress, "127.0.0.1")) {
                // 根据网卡取本机配置的IP
                ipAddress = getLocalIp();
            }
        }
        return ipAddress;
    }

    public static String getLocalIp() throws SocketException {
        Enumeration allNetInterfaces = NetworkInterface.getNetworkInterfaces();
        InetAddress ip;
        while (allNetInterfaces.hasMoreElements()) {
            NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
            Enumeration addresses = netInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                ip = (InetAddress) addresses.nextElement();
                if (ip instanceof Inet4Address) {
                    return ip.getHostAddress();
                }
            }
        }
        return "";
    }

    public static long localIpNumber() throws SocketException {
        return ipToLong(getLocalIp());
    }

    private static long ipToLong(String ipAddress) {
        long result = 0L;
        String[] ipAddressInArray = ipAddress.split("\\.");
        if (ipAddressInArray.length != 4) {
            return 0;
        }
        for (int i = 3; i >= 0; i--) {
            long ip = 0;
            try {
                ip = Long.parseLong(ipAddressInArray[3 - i]);
            } catch (NumberFormatException e) {
                // ignore
            }
            result |= ip << (i * 8);
        }
        return result;
    }

    public static String fetchPublicIp(List<String> ipAddressList) {
        String ipAddress = null;
        if (ipAddressList == null || ipAddressList.size() <= 0) {
            return ipAddress;
        }
        for (String ip : ipAddressList) {
            long ipNum = ipToLong(ip);
            if (isIpAddress(ip)
                    && !isInner(ipNum, aBegin, aEnd)
                    && !isInner(ipNum, bBegin, bEnd)
                    && !isInner(ipNum, cBegin, cEnd)
                    && !isInner(ipNum, saveBegin, saveEnd)) {
                ipAddress = ip;
                break;
            }
        }
        return ipAddress;
    }

    public static boolean isInnerIP(String ipAddress) {
        long ipNum = ipToLong(ipAddress);
        return isInner(ipNum, aBegin, aEnd) || isInner(ipNum, bBegin, bEnd) || isInner(ipNum, cBegin, cEnd) || isInner(ipNum, saveBegin, saveEnd);
    }

    private static boolean isInner(long userIp, long begin, long end) {
        return (userIp >= begin) && (userIp <= end);
    }

    /**
     * 检验是否是合法的IP地址
     *
     * @param address String IP地址
     * @return boolean IP地址是否合法
     */
    public static boolean isIpAddress(String address) {
        if (StringUtils.isEmpty(address)) {
            return false;
        }
        String regex = "((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(address);
        return m.matches();
    }

}
