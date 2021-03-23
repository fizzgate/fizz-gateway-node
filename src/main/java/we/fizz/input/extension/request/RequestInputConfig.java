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
import java.util.Map;

import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import we.fizz.input.InputConfig;



/**
 * 
 * @author linwaiwai
 * @author Francis Dong
 *
 */
public class RequestInputConfig extends InputConfig {
	private URL url ;
	private String method ;
	private int timeout = 3;
	
	public RequestInputConfig(Map configBody) {
		super(configBody);
		String url = (String) configBody.get("url");
		if(StringUtils.isEmpty(url)) {
			throw new RuntimeException("Request URL can not be blank");
		}
		setUrl(url);
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
	
	public String getQueryStr(){
		return url.getQuery();
	}
	
	public MultiValueMap<String, String> getQueryParams(){
		MultiValueMap<String, String> parameters =
	            UriComponentsBuilder.fromUriString(url.toString()).build().getQueryParams();
		return parameters;
	}

	
	public String getBaseUrl() {
		return url.getProtocol()+ "://"+ url.getHost() + (url.getPort() == -1 ? "" : ":" + url.getPort());
	}

	public String getPath() {
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

}
