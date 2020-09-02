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

package com.wehotel.config;

import com.wehotel.util.Constants;
import com.wehotel.util.Utils;

/**
 * @author lancer
 */
public abstract class RedisReactiveProperties {

    private String host = "127.0.0.1";
    private int port = 6379;
    private String password;
    private int database = 0;

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

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(48);
        appendTo(b);
        return b.toString();
    }

    public void appendTo(StringBuilder b) {
        b.append(Constants.Symbol.LEFT_BRACE);
        Utils.addTo(b, "host", Constants.Symbol.EQUAL, host, Constants.Symbol.SPACE_STR);
        Utils.addTo(b, "port", Constants.Symbol.EQUAL, port, Constants.Symbol.SPACE_STR);
        Utils.addTo(b, "password", Constants.Symbol.EQUAL, password, Constants.Symbol.SPACE_STR);
        Utils.addTo(b, "database", Constants.Symbol.EQUAL, database, Constants.Symbol.EMPTY);
        b.append(Constants.Symbol.RIGHT_BRACE);
    }
}
