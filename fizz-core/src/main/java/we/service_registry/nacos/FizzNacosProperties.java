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

package we.service_registry.nacos;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.nacos.api.naming.PreservedMetadataKeys;
import com.alibaba.nacos.client.naming.utils.UtilAndComs;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

import static com.alibaba.nacos.api.PropertyKeyConst.*;

/**
 * @author hongqiaowei
 */

public class FizzNacosProperties extends NacosDiscoveryProperties {

    private ApplicationContext applicationContext;

    private String id;

    private String serverAddr;

    private String username = "";

    private String password = "";

    private String endpoint = "";

    private String namespace;

    private long watchDelay = 30000;

    private String logName = "";

    private String service;

    private float weight = 1;

    private String clusterName = "DEFAULT";

    private String group = "DEFAULT_GROUP";

    private String namingLoadCacheAtStart = "false";

    private Map<String, String> metadata = new HashMap<>();

    private boolean registerEnabled = true;

    private String ip;

    private String networkInterface = "";

    private int port = -1;

    private boolean secure = false;

    private String accessKey = "";

    private String secretKey = "";

    private Integer heartBeatInterval;

    private Integer heartBeatTimeout;

    private Integer ipDeleteTimeout;

    private boolean instanceEnabled = true;

    private boolean ephemeral = true;

    private boolean failFast = true;

    private boolean init = false;

    private Properties config;

    public FizzNacosProperties(Properties config) {
        this.config = config;
    }

    public void init() {
        if (init) {
            return;
        }

        metadata.put(PreservedMetadataKeys.REGISTER_SOURCE, "SPRING_CLOUD");
        if (secure) {
            metadata.put("secure", "true");
        }

        serverAddr = Objects.toString(serverAddr, "");
        if (serverAddr.endsWith("/")) {
            serverAddr = serverAddr.substring(0, serverAddr.length() - 1);
        }
        endpoint = Objects.toString(endpoint, "");
        namespace = Objects.toString(namespace, "");
        logName = Objects.toString(logName, "");

        if (StringUtils.isEmpty(ip)) {
            if (StringUtils.isEmpty(networkInterface)) {
                InetUtils inetUtils = null;
                try {
                    inetUtils = applicationContext.getBean(InetUtils.class);
                } catch (NoSuchBeanDefinitionException e) {
                    inetUtils = new InetUtils(new InetUtilsProperties());
                }
                ip = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
            } else {
                NetworkInterface netInterface = null;
                try {
                    netInterface = NetworkInterface.getByName(networkInterface);
                } catch (SocketException e) {
                    throw new RuntimeException(e);
                }
                Enumeration<InetAddress> inetAddress = netInterface.getInetAddresses();
                while (inetAddress.hasMoreElements()) {
                    InetAddress currentAddress = inetAddress.nextElement();
                    if (currentAddress instanceof Inet4Address
                            && !currentAddress.isLoopbackAddress()) {
                        ip = currentAddress.getHostAddress();
                        break;
                    }
                }
                if (StringUtils.isEmpty(ip)) {
                    throw new RuntimeException("cannot find available ip from"
                            + " network interface " + networkInterface);
                }
            }
        }

        init = true;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getLogName() {
        return logName;
    }

    public void setLogName(String logName) {
        this.logName = logName;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public boolean isRegisterEnabled() {
        return registerEnabled;
    }

    public void setRegisterEnabled(boolean registerEnabled) {
        this.registerEnabled = registerEnabled;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNetworkInterface() {
        return networkInterface;
    }

    public void setNetworkInterface(String networkInterface) {
        this.networkInterface = networkInterface;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Integer getHeartBeatInterval() {
        return heartBeatInterval;
    }

    public void setHeartBeatInterval(Integer heartBeatInterval) {
        this.heartBeatInterval = heartBeatInterval;
    }

    public Integer getHeartBeatTimeout() {
        return heartBeatTimeout;
    }

    public void setHeartBeatTimeout(Integer heartBeatTimeout) {
        this.heartBeatTimeout = heartBeatTimeout;
    }

    public Integer getIpDeleteTimeout() {
        return ipDeleteTimeout;
    }

    public void setIpDeleteTimeout(Integer ipDeleteTimeout) {
        this.ipDeleteTimeout = ipDeleteTimeout;
    }

    public String getNamingLoadCacheAtStart() {
        return namingLoadCacheAtStart;
    }

    public void setNamingLoadCacheAtStart(String namingLoadCacheAtStart) {
        this.namingLoadCacheAtStart = namingLoadCacheAtStart;
    }

    public long getWatchDelay() {
        return watchDelay;
    }

    public void setWatchDelay(long watchDelay) {
        this.watchDelay = watchDelay;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isInstanceEnabled() {
        return instanceEnabled;
    }

    public void setInstanceEnabled(boolean instanceEnabled) {
        this.instanceEnabled = instanceEnabled;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public void setEphemeral(boolean ephemeral) {
        this.ephemeral = ephemeral;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FizzNacosProperties that = (FizzNacosProperties) o;
        return Objects.equals(serverAddr, that.serverAddr)
                && Objects.equals(username, that.username)
                && Objects.equals(password, that.password)
                && Objects.equals(endpoint, that.endpoint)
                && Objects.equals(namespace, that.namespace)
                && Objects.equals(logName, that.logName)
                && Objects.equals(service, that.service)
                && Objects.equals(clusterName, that.clusterName)
                && Objects.equals(group, that.group) && Objects.equals(ip, that.ip)
                && Objects.equals(port, that.port)
                && Objects.equals(networkInterface, that.networkInterface)
                && Objects.equals(accessKey, that.accessKey)
                && Objects.equals(secretKey, that.secretKey)
                && Objects.equals(heartBeatInterval, that.heartBeatInterval)
                && Objects.equals(heartBeatTimeout, that.heartBeatTimeout)
                && Objects.equals(failFast, that.failFast)
                && Objects.equals(ipDeleteTimeout, that.ipDeleteTimeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverAddr, username, password, endpoint, namespace,
                watchDelay, logName, service, weight, clusterName, group,
                namingLoadCacheAtStart, registerEnabled, ip, networkInterface, port,
                secure, accessKey, secretKey, heartBeatInterval, heartBeatTimeout,
                ipDeleteTimeout, instanceEnabled, ephemeral, failFast);
    }

    @Override
    public String toString() {
        return "FizzNacosProperties{" + "serverAddr='" + serverAddr + '\''
                + ", endpoint='" + endpoint + '\'' + ", namespace='" + namespace + '\''
                + ", watchDelay=" + watchDelay + ", logName='" + logName + '\''
                + ", service='" + service + '\'' + ", weight=" + weight
                + ", clusterName='" + clusterName + '\'' + ", group='" + group + '\''
                + ", namingLoadCacheAtStart='" + namingLoadCacheAtStart + '\''
                + ", metadata=" + metadata + ", registerEnabled=" + registerEnabled
                + ", ip='" + ip + '\'' + ", networkInterface='" + networkInterface + '\''
                + ", port=" + port + ", secure=" + secure + ", accessKey='" + accessKey
                + '\'' + ", secretKey='" + secretKey + '\'' + ", heartBeatInterval="
                + heartBeatInterval + ", heartBeatTimeout=" + heartBeatTimeout
                + ", ipDeleteTimeout=" + ipDeleteTimeout + ", failFast=" + failFast + '}';
    }

    @Override
    public Properties getNacosProperties() {
        Properties properties = new Properties();
        properties.put(SERVER_ADDR, serverAddr);
        properties.put(USERNAME, Objects.toString(username, ""));
        properties.put(PASSWORD, Objects.toString(password, ""));
        properties.put(NAMESPACE, namespace);
        properties.put(UtilAndComs.NACOS_NAMING_LOG_NAME, logName);

        if (endpoint.contains(":")) {
            int index = endpoint.indexOf(":");
            properties.put(ENDPOINT, endpoint.substring(0, index));
            properties.put(ENDPOINT_PORT, endpoint.substring(index + 1));
        } else {
            properties.put(ENDPOINT, endpoint);
        }

        properties.put(ACCESS_KEY, accessKey);
        properties.put(SECRET_KEY, secretKey);
        properties.put(CLUSTER_NAME, clusterName);
        properties.put(NAMING_LOAD_CACHE_AT_START, namingLoadCacheAtStart);

        properties.put("enabled", true);
        // properties.put("server-addr", serverAddr);
        properties.put("com.alibaba.nacos.naming.log.filename", "");

        config.forEach(
                (c, v) -> {
                    if (!properties.containsKey(c)) {
                        properties.put(c, v);
                    }
                }
        );

        return properties;
    }
}
