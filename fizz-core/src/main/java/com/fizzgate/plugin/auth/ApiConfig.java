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

package com.fizzgate.plugin.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fizzgate.plugin.PluginConfig;
import com.fizzgate.proxy.Route;
import com.fizzgate.util.Consts;
import com.fizzgate.util.JacksonUtils;
import com.fizzgate.util.UrlTransformUtils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author hongqiaowei
 */

public class ApiConfig {

    public static interface Type {
        static final byte UNDEFINED         = 0;
        static final byte SERVICE_AGGREGATE = 1;
        static final byte SERVICE_DISCOVERY = 2;
        static final byte REVERSE_PROXY     = 3;
        static final byte CALLBACK          = 4;
        static final byte DUBBO             = 5;
        static final byte DIRECT_RESPONSE   = 6;
    }

    public  static final String ALL_METHOD     = "AM";

    private static final String match_all      = "/**";

    @JsonProperty(
    access = JsonProperty.Access.WRITE_ONLY
    )
    public  int                id;

    @JsonProperty(
    access = JsonProperty.Access.WRITE_ONLY
    )
    public  boolean            isDeleted          = false;

    public  Set<String>        gatewayGroups      = Stream.of(GatewayGroup.DEFAULT).collect(Collectors.toCollection(LinkedHashSet::new));

    @JsonProperty(
    access = JsonProperty.Access.WRITE_ONLY
    )
    public  String             firstGatewayGroup  = GatewayGroup.DEFAULT;

    public  String             service;

    public  String             registryCenter;

    public  String             backendService;

    public  Object             fizzMethod         = ALL_METHOD;

    public  String             path               = match_all;

    @JsonProperty(
    access = JsonProperty.Access.WRITE_ONLY
    )
    public  boolean            exactMatch         = false;

    public  String             backendPath;

    public  boolean            dedicatedLine      = false;

    @JsonProperty("proxyMode")
    public  byte               type               = Type.SERVICE_DISCOVERY;

    private int                counter            = 0;

    public  List<String>       httpHostPorts;

    public  boolean            allowAccess        = true;

    public  List<PluginConfig> pluginConfigs      = Collections.emptyList();

    public  boolean            checkApp           = false;

    public  CallbackConfig     callbackConfig;

    public  String             rpcMethod;

    public  String             rpcParamTypes;

    public  String             rpcVersion;

    public  String             rpcGroup;

    public  long               timeout            = 0;

    public  int                retryCount         = 0;

    public  long               retryInterval      = 0;

    public void setDeleted(int v) {
        if (v == 1) {
            isDeleted = true;
        }
    }

    public void setAccess(char c) {
        if (c != 'a') {
            allowAccess = false;
        }
    }

    public void setGatewayGroup(String ggs) {
        gatewayGroups.remove(GatewayGroup.DEFAULT);
        if (StringUtils.isBlank(ggs)) {
            gatewayGroups.add("*");
        } else {
            Arrays.stream(StringUtils.split(ggs, ',')).forEach(
                    gg -> {
                        gatewayGroups.add(gg.trim());
                    }
            );
        }
        firstGatewayGroup = gatewayGroups.iterator().next();
    }

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

    public void setPath(String p) {
        if (StringUtils.isNotBlank(p)) {
            if ("/".equals(p)) {
                path = match_all;
            } else {
                path = p.trim();
                if (!UrlTransformUtils.isAntPathPattern(path)) {
                    exactMatch = true;
                }
            }
        } else {
            path = match_all;
        }
    }

    public void setMethod(String m) {
        fizzMethod = HttpMethod.resolve(m);
        if (fizzMethod == null) {
            fizzMethod = ALL_METHOD;
        }
    }

    public void setAppEnable(int v) {
        if (v == 1) {
            checkApp = true;
        }
    }

    public void setDedicatedLine(int v) {
        if (v == 1) {
            dedicatedLine = true;
        }
    }

    @JsonIgnore
    public String getNextHttpHostPort() {
        int i = counter++;
        if (i < 0) {
            i = Math.abs(i);
        }
        return httpHostPorts.get(
            i % httpHostPorts.size()
        );
    }

    public String transform(String reqPath) {
        if (exactMatch) {
            return backendPath;
        }
        return UrlTransformUtils.transform(path, backendPath, reqPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ApiConfig) {
            ApiConfig that = (ApiConfig) o;
            return this.id == that.id;
        }
        return false;
    }

    public Route getRoute(ServerWebExchange exchange, @Nullable List<PluginConfig> gatewayGroupPluginConfigs) {
        ServerHttpRequest request = exchange.getRequest();
        Route r = new Route().dedicatedLine(  this.dedicatedLine)
                             .type(           this.type)
                             .method(         request.getMethod())
                             .path(           this.path)
                             .registryCenter( this.registryCenter)
                             .backendService( this.backendService)
                             .backendPath(    this.backendPath)
                             .rpcMethod(      this.rpcMethod)
                             .rpcParamTypes(  this.rpcParamTypes)
                             .rpcGroup(       this.rpcGroup)
                             .rpcVersion(     this.rpcVersion)
                             .timeout(        this.timeout)
                             .retryCount(     this.retryCount)
                             .retryInterval(  this.retryInterval);

        if (gatewayGroupPluginConfigs == null || gatewayGroupPluginConfigs.isEmpty()) {
            r.pluginConfigs = this.pluginConfigs;
        } else {
            List<PluginConfig> pcs = new ArrayList<>(gatewayGroupPluginConfigs.size() + this.pluginConfigs.size());
            pcs.addAll(gatewayGroupPluginConfigs);
            pcs.addAll(this.pluginConfigs);
            pcs.sort(null);
            r.pluginConfigs = pcs;
        }

        if (this.type == Type.REVERSE_PROXY) {
            r = r.nextHttpHostPort(getNextHttpHostPort());
        }

        return r;
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
