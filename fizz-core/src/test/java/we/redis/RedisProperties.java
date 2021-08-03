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

package we.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * @author hongqiaowei
 */

@TestConfiguration
public class RedisProperties {

    private String host;
    private int port;
    private int database;

    public RedisProperties(
            @Value("${embeded.redis.port}") int port,
            @Value("${embeded.redis.host}") String host,
            @Value("${embeded.redis.database}") int database) {
        this.port = port;
        this.host = host;
        this.database = database;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int redisPort) {
        this.port = redisPort;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String redisHost) {
        this.host = redisHost;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    @Override
    public String toString() {
        return "redis:[host:" + host + ",port:" + port + ",database:" + database + "]";
    }
}
