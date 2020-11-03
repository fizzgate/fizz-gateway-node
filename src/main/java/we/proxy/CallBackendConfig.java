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

package we.proxy;

/**
 * @author hongqiaowei
 * 请求转发/调用后端接口时的负载均衡、流控、failover、超时等配置
 */

public class CallBackendConfig {

    // TODO to be continue

    public static final long DEFAULT_TIME_OUT = 3000; // mills

    public static final CallBackendConfig DEFAULT = new CallBackendConfig();

    public long timeout = DEFAULT_TIME_OUT;

    public CallBackendConfig() {
    }

    public CallBackendConfig(long timeout) {
        this.timeout = timeout;
    }
}
