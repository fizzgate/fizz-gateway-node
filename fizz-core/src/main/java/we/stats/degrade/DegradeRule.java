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
package we.stats.degrade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import we.util.JacksonUtils;

import static we.util.ResourceIdUtils.SERVICE_DEFAULT;

/**
 * Degrade rule entity
 *
 * @author zhongjie
 */
@Data
public class DegradeRule {
    /**
     * 整型自增主键
     */
    private Long id;
    /**
     * 熔断类型 1-服务默认配置 2-服务 3-接口
     */
    private Byte type;
    /**
     * 前端服务名
     */
    private String service;
    /**
     * 前端API路径
     */
    private String path;
    /**
     * 熔断策略 1-异常比例 2-异常数
     */
    private Byte strategy;
    /**
     * 比例阈值，当熔断策略为 1-异常比例 时该字段有值
     */
    private Float ratioThreshold;
    /**
     * 异常数，当熔断策略为 2-异常数 时该字段有值
     */
    private Long exceptionCount;
    /**
     * 最小请求数
     */
    private Long minRequestCount;
    /**
     * 熔断时长（秒）
     */
    private Integer timeWindow;
    /**
     * 统计时长（秒）
     */
    private Integer statInterval;
    /**
     * 恢复策略 1-尝试恢复 2-逐步恢复 3-立即恢复
     */
    private Byte recoveryStrategy;
    /**
     * 恢复时长（秒），当恢复策略为 2-逐步恢复 时该字段有值
     */
    private Integer recoveryTimeWindow;
    /**
     * 熔断响应ContentType
     */
    private String responseContentType;
    /**
     * 熔断响应报文
     */
    private String responseContent;

    /**
     * 当type为1时，1启用，0反之
     */
    private Integer enable;
    /**
     * 是否删除 1-是 2-否
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
