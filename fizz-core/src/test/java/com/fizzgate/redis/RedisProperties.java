package com.fizzgate.redis;

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
