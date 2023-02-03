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
package com.fizzgate.stats.degrade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fizzgate.util.JacksonUtils;

import lombok.Data;

import static com.fizzgate.util.ResourceIdUtils.SERVICE_DEFAULT;

import org.apache.commons.lang3.StringUtils;

/**
 * Degrade rule entity
 *
 * @author zhongjie
 */
@Data
public class DegradeRule {
    /**
     * ID
     */
    private Long id;
    /**
     * Degrade type: 1-default service 2-service 3-path
     */
    private Byte type;
    /**
     * Front service name
     */
    private String service;
    /**
     * Front API path
     */
    private String path;
    /**
     * Degrade strategy: 1-exception ratio 2-exception count
     */
    private Byte strategy;
    /**
     * Ratio threshold, not null when degrade strategy is 1-exception ratio
     */
    private Float ratioThreshold;
    /**
     * Exception count, not null when degrade strategy is 2-exception count
     */
    private Long exceptionCount;
    /**
     * Minimal request count
     */
    private Long minRequestCount;
    /**
     * Time window(second)
     */
    private Integer timeWindow;
    /**
     * Statistic interval(second)
     */
    private Integer statInterval;
    /**
     * Recovery strategy: 1-try one 2-recover gradually 3-recover immediately
     */
    private Byte recoveryStrategy;
    /**
     * Recovery time window(second)，not null when recovery strategy is 2-recover gradually
     */
    private Integer recoveryTimeWindow;
    /**
     * Response ContentType
     */
    private String responseContentType;
    /**
     * Response Content
     */
    private String responseContent;

    /**
     * When type is 1-default service，1-enable 0-disable
     */
    private Integer enable;
    /**
     * Is deleted, 1-yes 2-no
     */
    private Integer isDeleted;

    private String resourceId;

    public boolean isDeleted() {
        return isDeleted == 1;
    }

    public boolean isEnable() {
        return enable == 1;
    }

    public void setType(Byte type) {
        this.type = type;
        if (type == 1) {
            service = SERVICE_DEFAULT;
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


    @JsonIgnore
    public String getResourceId() {
        if (resourceId == null) {
            resourceId = "^^^" + (service == null ? "" : service) + '^' + (path == null ? "" : path);
        }
        return resourceId;
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
