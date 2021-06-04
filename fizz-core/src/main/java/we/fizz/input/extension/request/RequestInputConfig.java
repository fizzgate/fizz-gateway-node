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

package we.fizz.input.extension.request;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.noear.snack.ONode;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import we.fizz.input.InputConfig;
import we.fizz.input.PathMapping;



/**
 * 
 * @author linwaiwai
 * @author Francis Dong
 *
 */
public class RequestInputConfig extends InputConfig {
	private URL url ;
	private String method ;
	private int timeout;
	private String protocol;
	/**
	 * Service Type, 1 service discovery, 2 HTTP service
	 */
	private Integer serviceType;
	private String serviceName;
	private String path;
	
	public RequestInputConfig(Map configBody) {
		super(configBody);
		
		if (configBody.get("serviceType") != null && StringUtils.isNotBlank((String) configBody.get("protocol"))
				&& StringUtils.isNotBlank((String) configBody.get("serviceName"))
				&& StringUtils.isNotBlank((String) configBody.get("path"))) {
			serviceType = Integer.valueOf(configBody.get("serviceType").toString());
			protocol = ((String) configBody.get("protocol")).toLowerCase();
			serviceName = (String) configBody.get("serviceName");
			path = (String) configBody.get("path");
		} else {
			String url = (String) configBody.get("url");
			if (StringUtils.isBlank(url)) {
				throw new RuntimeException("Request URL can not be blank");
			}
			setUrl(url);
		}
		
		if (configBody.get("method") != null) {
			setMethod(((String)configBody.get("method")).toUpperCase());
		} else {
			setMethod("GET");
		}
		if (configBody.get("timeout") != null) {
			timeout = Integer.valueOf(configBody.get("timeout").toString());
		}
		if (configBody.get("fallback") != null) {
			Map<String,String> fallback = (Map<String,String>)configBody.get("fallback");
			setFallback(fallback);
		}
		if (configBody.get("condition") != null) {
			setCondition((Map)configBody.get("condition"));
		}
	}
	
	public boolean isNewVersion() {
		if (serviceType != null && StringUtils.isNotBlank(protocol) && StringUtils.isNotBlank(serviceName)
				&& StringUtils.isNotBlank(path)) {
			return true;
		}
		return false;
	}
	
	public String getQueryStr(){
		return url.getQuery();
	}
	
	public MultiValueMap<String, String> getQueryParams(){
		if (isNewVersion()) {
			return null;
		}
		MultiValueMap<String, String> parameters =
	            UriComponentsBuilder.fromUriString(url.toString()).build().getQueryParams();
		return parameters;
	}

	
	public String getBaseUrl() {
		return url.getProtocol()+ "://"+ url.getHost() + (url.getPort() == -1 ? "" : ":" + url.getPort());
	}

	public String getPath() {
		if (isNewVersion()) {
			return this.path;
		}
		return url.getPath();
	}

	public void setUrl(String string) {
		try {
			url = new URL(string);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public RequestInputConfig() {
		super(null);
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public Integer getServiceType() {
		return serviceType;
	}

	public void setServiceType(Integer serviceType) {
		this.serviceType = serviceType;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
