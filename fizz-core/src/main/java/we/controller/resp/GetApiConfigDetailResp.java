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
package we.controller.resp;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Get api config detail response entity
 *
 * @author zhongjie
 * @since 2.6.0
 */
@Data
public class GetApiConfigDetailResp implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Is exist
     */
    private Boolean exist;

    /**
     * ID
     */
    private Long id;
    /**
     * Gateway group
     */
    private String gatewayGroup;
    /**
     * Frontend service name
     */
    private String service;
    /**
     * Method
     */
    private String method;
    /**
     * Frontend API path
     */
    private String path;

    /**
     * Is app Enabled
     */
    private Boolean appEnabled;
    /**
     * App set
     */
    private Set<String> apps;

    /**
     * Can access ? a-allow, f-forbid
     */
    private String access;
    /**
     * Proxy mode: 1-aggregate 2-discovery 3-proxy 4-callback 5-Dubbo
     */
    private Byte proxyMode;

    /**
     * Backend service name
     */
    private String backendService;
    /**
     * Backend API path
     */
    private String backendPath;

    /**
     * Api plugin list
     */
    private List<ApiPluginVO> apiPlugins;

    /**
     * Api backend list
     */
    private List<String> apiBackends;
    /**
     * RPC method
     */
    private String rpcMethod;
    /**
     * RPC parameter types
     */
    private String rpcParamTypes;
    /**
     * RPC version
     */
    private String rpcVersion;
    /**
     * RPC group
     */
    private String rpcGroup;
    /**
     * Timeout millis
     */
    private Integer timeout;

    /**
     * Retry count
     */
    private Integer retryCount;
    /**
     * Retry interval millis
     */
    private Long retryInterval;

    /**
     * Is dedicated line: 0-no 1-yes
     */
    private Byte isDedicatedLine;

    /**
     * Registry name
     */
    private String registryName;

    @Data
    public static class ApiPluginVO implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Plugin english name
         */
        public String plugin;
        /**
         * Config map
         */
        public Map<String, Object> config;
        /**
         * Order
         */
        public Integer order;
    }
}
