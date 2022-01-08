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

package we.stats.circuitbreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@Component
public class CircuitBreakManager {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakManager.class);

    private Map<String/*resource*/, CircuitBreaker> circuitBreakerMap = new HashMap<>(32);

    @PostConstruct
    public void init() {
        if (!circuitBreakerMap.isEmpty()) {
            schedule();
        }
    }

    private CircuitBreaker create(Object circuitBreakerConfig) {
        // circuitBreakerConfig => CircuitBreaker
        circuitBreakerMap.put("", null/*CircuitBreaker*/);
        return null;
    }

    public boolean permit(ServerWebExchange exchange) {
        return false;
    }

    private void schedule() {
        // 每隔一秒，对所有资源
        // 采集统计信息
        // CircuitBreaker.correctState();
    }
}
