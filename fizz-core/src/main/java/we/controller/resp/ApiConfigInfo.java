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

/**
 * Api config entity
 *
 * @author zhongjie
 * @since 2.6.0
 */
@Data
public class ApiConfigInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * Identifier
     */
    private Long id;
    /**
     * Gateway group
     */
    private String gatewayGroup;
    /**
     * Proxy mode: 1-aggregate 2-discovery 3-proxy 4-callback 5-Dubbo
     */
    private Byte proxyMode;
    /**
     * Is dedicated line: 0-no 1-yes
     */
    private Byte isDedicatedLine;
    /**
     * Frontend service name
     */
    private String service;
    /**
     * Frontend API path
     */
    private String path;
    /**
     * Method
     */
    private String method;
    /**
     * Can access ? a-allow，f-forbid
     */
    private String access;
    /**
     * Plugin english names
     */
    private List<String> plugins;
}