/*
 *  Copyright (C) 2021 the original author or authors.
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
package we.fizz.input.extension.dubbo;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import we.fizz.exception.FizzRuntimeException;
import we.fizz.input.InputConfig;

/**
*
* @author linwaiwai
* @author Francis Dong
*
*/
public class DubboInputConfig extends InputConfig {
	private String serviceName;
	private String version;
	private String group;
	private String method;
	private String paramTypes;
	private int timeout;

	public DubboInputConfig(Map configMap) {
		super(configMap);
	}

	public void parse() {
		String serviceName = (String) configMap.get("serviceName");
		if (StringUtils.isBlank(serviceName)) {
			throw new FizzRuntimeException("service name can not be blank");
		}
		setServiceName(serviceName);
		
		String version = (String) configMap.get("version");
		if (!StringUtils.isBlank(version)) {
			setVersion(version);
		}
		
		String group = (String) configMap.get("group");
		if (!StringUtils.isBlank(group)) {
			setGroup(group);
		}
		
		String method = (String) configMap.get("method");
		if (StringUtils.isBlank(method)) {
			throw new FizzRuntimeException("method can not be blank");
		}
		setMethod(method);
		String paramTypes = (String) configMap.get("paramTypes");
		if (!StringUtils.isBlank(paramTypes)) {
			setParamTypes(paramTypes);
		}

		if (configMap.get("timeout") != null && StringUtils.isNotBlank(configMap.get("timeout").toString())) {
			try {
				setTimeout(Integer.valueOf(configMap.get("timeout").toString()));
			} catch (Exception e) {
				throw new RuntimeException("invalid timeout: " + configMap.get("timeout").toString() + " " + e.getMessage(), e);
			}
		}
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getParamTypes() {
		return paramTypes;
	}

	public void setParamTypes(String paramTypes) {
		this.paramTypes = paramTypes;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getServiceName() {
		return serviceName;
	}

	public String getMethod() {
        return method;
    }

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

}
