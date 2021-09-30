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

import we.util.Constants;
import we.util.Consts;
import we.util.Utils;

/**
 * @author hongqiaowei
 */

public abstract class RedisReactiveProperties {

    private String host      = "127.0.0.1";
    private int    port      = 6379;
    private String password;
    private int    database  = 0;

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
        b.append(Consts.S.LEFT_BRACE);
        Utils.addTo(b, "host",     Consts.S.EQUAL, host,     Consts.S.SPACE_STR);
        Utils.addTo(b, "port",     Consts.S.EQUAL, port,     Consts.S.SPACE_STR);
//      Utils.addTo(b, "password",    Consts.S.EQUAL, password, Consts.S.SPACE_STR);
        Utils.addTo(b, "database", Consts.S.EQUAL, database, Consts.S.EMPTY);
        b.append(Consts.S.RIGHT_BRACE);
    }
}
