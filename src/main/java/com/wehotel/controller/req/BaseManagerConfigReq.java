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

package com.wehotel.controller.req;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * 管理后台聚合配置相关请求基类
 * @author zhongjie
 */
public abstract class BaseManagerConfigReq implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int MAX_TIME_DEVIATION_MINUTES = 10;
    private static final int MAX_TIME_DEVIATION_MILLIS = MAX_TIME_DEVIATION_MINUTES * 60 * 1000;

    @NotNull(message = "签名不能为空")
    private String sign;
    @NotNull(message = "时间戳不能为空")
    private Long timestamp;

    public boolean checkSign(String key) {
        long currentTimestamp = System.currentTimeMillis();
        if (Math.abs(currentTimestamp - timestamp) > MAX_TIME_DEVIATION_MILLIS) {
            throw new RuntimeException(String.format("请求时间戳[%s]和当前系统时间[%s]相差超过%s分钟", timestamp,
                    currentTimestamp, MAX_TIME_DEVIATION_MINUTES));
        }
        return this.innerCheckSign(key, sign, timestamp);
    }

    /**
     * 校验签名
     * @param key 秘钥
     * @param sign 签名
     * @param timestamp 时间戳
     * @return TRUE：检验通过
     */
    abstract boolean innerCheckSign(String key, String sign, Long timestamp);

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseManagerConfigReq that = (BaseManagerConfigReq) o;
        return Objects.equals(sign, that.sign) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sign, timestamp);
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
