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

package we.config;

import io.lettuce.core.ReadFrom;
import org.springframework.data.redis.connection.RedisNode;
import we.util.Consts;
import we.util.StringUtils;
import we.util.Utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hongqiaowei
 */

public abstract class RedisReactiveProperties {

    public static final String STANDALONE = "standalone";
    public static final String CLUSTER    = "cluster";

    private String          type                              = STANDALONE;

    private String          host                              = "127.0.0.1";

    private int             port                              = 6379;

    private String          password;

    private int             database                          = 0;

    private List<RedisNode> clusterNodes;

    private int             maxRedirects                      = 0;

    private int             clusterRefreshPeriod              = 60;

    private boolean         clusterRefreshAdaptive            = true;

    private boolean         enableAllAdaptiveRefreshTriggers  = true;

    private ReadFrom        readFrom                          = ReadFrom.REPLICA_PREFERRED;

    private int             minIdle                           = 0;

    private int             maxIdle                           = 0;

    private int             maxTotal                          = 0;

    private Duration        maxWait;

    private Duration        timeBetweenEvictionRuns;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type.equals(STANDALONE)) {
            this.type = STANDALONE;
        } else {
            this.type = CLUSTER;
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public List<RedisNode> getClusterNodes() {
        return clusterNodes;
    }

    public void setClusterNodes(String clusterNodes) {
        String[] nodeArr = StringUtils.split(clusterNodes, ',');
        this.clusterNodes = new ArrayList<>(nodeArr.length);
        for (String n : nodeArr) {
            String[] ipAndPort = StringUtils.split(n.trim(), ':');
            RedisNode redisNode = new RedisNode(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
            this.clusterNodes.add(redisNode);
        }
    }

    public int getMaxRedirects() {
        return maxRedirects;
    }

    public void setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
    }

    public int getClusterRefreshPeriod() {
        return clusterRefreshPeriod;
    }

    public void setClusterRefreshPeriod(int clusterRefreshPeriod) {
        this.clusterRefreshPeriod = clusterRefreshPeriod;
    }

    public boolean isClusterRefreshAdaptive() {
        return clusterRefreshAdaptive;
    }

    public void setClusterRefreshAdaptive(boolean clusterRefreshAdaptive) {
        this.clusterRefreshAdaptive = clusterRefreshAdaptive;
    }

    public boolean isEnableAllAdaptiveRefreshTriggers() {
        return enableAllAdaptiveRefreshTriggers;
    }

    public void setEnableAllAdaptiveRefreshTriggers(boolean enableAllAdaptiveRefreshTriggers) {
        this.enableAllAdaptiveRefreshTriggers = enableAllAdaptiveRefreshTriggers;
    }

    public ReadFrom getReadFrom() {
        return readFrom;
    }

    public void setReadFrom(String readFrom) {
        this.readFrom = ReadFrom.valueOf(readFrom);
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public Duration getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(long maxWait) {
        this.maxWait = Duration.ofMillis(maxWait);
    }

    public Duration getTimeBetweenEvictionRuns() {
        return timeBetweenEvictionRuns;
    }

    public void setTimeBetweenEvictionRuns(long timeBetweenEvictionRuns) {
        this.timeBetweenEvictionRuns = Duration.ofMillis(timeBetweenEvictionRuns);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(256);
        appendTo(b);
        return b.toString();
    }

    public void appendTo(StringBuilder b) {
        b.append(Consts.S.LEFT_BRACE);
        if (type == STANDALONE) {
            Utils.addTo(b, "host",     Consts.S.EQUAL, host,     Consts.S.SPACE_STR);
            Utils.addTo(b, "port",     Consts.S.EQUAL, port,     Consts.S.SPACE_STR);
            Utils.addTo(b, "database", Consts.S.EQUAL, database, Consts.S.SPACE_STR);
        } else {
            Utils.addTo(b, "clusterNodes",                     Consts.S.EQUAL, clusterNodes,                     Consts.S.SPACE_STR);
            Utils.addTo(b, "maxRedirects",                     Consts.S.EQUAL, maxRedirects,                     Consts.S.SPACE_STR);
            Utils.addTo(b, "clusterRefreshPeriod",             Consts.S.EQUAL, clusterRefreshPeriod,             Consts.S.SPACE_STR);
            Utils.addTo(b, "clusterRefreshAdaptive",           Consts.S.EQUAL, clusterRefreshAdaptive,           Consts.S.SPACE_STR);
            Utils.addTo(b, "enableAllAdaptiveRefreshTriggers", Consts.S.EQUAL, enableAllAdaptiveRefreshTriggers, Consts.S.SPACE_STR);
            Utils.addTo(b, "readFrom",                         Consts.S.EQUAL, readFrom,                         Consts.S.SPACE_STR);
        }

        Utils.addTo(b, "minIdle",                 Consts.S.EQUAL, minIdle,                 Consts.S.SPACE_STR);
        Utils.addTo(b, "maxIdle",                 Consts.S.EQUAL, maxIdle,                 Consts.S.SPACE_STR);
        Utils.addTo(b, "maxWait",                 Consts.S.EQUAL, maxWait,                 Consts.S.SPACE_STR);
        Utils.addTo(b, "maxTotal",                Consts.S.EQUAL, maxTotal,                Consts.S.SPACE_STR);
        Utils.addTo(b, "timeBetweenEvictionRuns", Consts.S.EQUAL, timeBetweenEvictionRuns, Consts.S.EMPTY);
        b.append(Consts.S.RIGHT_BRACE);
    }
}
