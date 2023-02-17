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

package com.fizzgate.controller.req;

import org.springframework.util.DigestUtils;

import javax.validation.constraints.NotBlank;
import java.util.Objects;

/**
 * 获取聚合配置JSON字符串请求实体类
 * @author zhongjie
 */
public class GetConfigStrReq extends BaseManagerConfigReq {
    /**
     * 配置ID
     */
    @NotBlank(message = "配置ID不能为空")
    private String configId;

    @Override
    boolean innerCheckSign(String key, String sign, Long timestamp) {
        return DigestUtils.md5DigestAsHex(
                String.format("%s-%s-%s", configId, timestamp, key).getBytes()).equals(sign);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        GetConfigStrReq that = (GetConfigStrReq) o;
        return Objects.equals(configId, that.configId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), configId);
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }
}
