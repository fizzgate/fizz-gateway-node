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

package we.fizz.input.extension.mysql;

import com.alibaba.fastjson.JSON;
import org.springframework.util.StringUtils;
import we.fizz.exception.FizzRuntimeException;
import we.fizz.input.InputConfig;

import java.util.Map;
/**
 * 
 * @author linwaiwai
 *
 */
public class MySQLInputConfig extends InputConfig {
	private String URL;
	private Map<String, Object> binds;
	private String sql;
	public MySQLInputConfig(Map configBody) {
		super(configBody);
	}

	public String getURL() {
		return URL;
	}

	public void setURL(String url) {
		this.URL = url;
	}

    public Map<String, Object> getBinds() {
        return binds;
    }

    public void setBinds(Map<String, Object> binds) {
        this.binds = binds;
    }

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public void parse(){
		String URL = (String) configMap.get("URL");
		if (StringUtils.isEmpty(URL)) {
			throw new FizzRuntimeException("service name can not be blank");
		}
		setURL(URL);

		String statement = (String) configMap.get("sql");
		if (StringUtils.isEmpty(statement)) {
			throw new FizzRuntimeException("sql can not be blank");
		}
		setSql(statement);

		if (configMap.get("binds") != null) {
			String bindsStr = (String) configMap.get("binds");
			Map<String, Object> binds = (Map<String, Object>) JSON.parse(bindsStr);
			setBinds(binds);
		}
	}
}
