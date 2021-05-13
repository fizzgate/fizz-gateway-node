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

package we.fizz.input;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author linwaiwai
 * @author Francis Dong
 *
 */
public class ClientInputConfig extends InputConfig {
	
	private boolean debug;
	private String path;
	private String method;
	private Map<String, Object> headers = new HashMap<String, Object>();
	private Map<String, Object> langDef;
	private Map<String, Object> bodyDef;
    private Map<String, Object> headersDef;
    private Map<String, Object> paramsDef;
    private Map<String, Object> scriptValidate;
    private Map<String, Object> validateResponse;
    private String contentType;
    private String xmlArrPaths;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ClientInputConfig(Map configBody) {
		super(configBody);
		if(configBody.get("debug") != null) {
			this.debug = (boolean) configBody.get("debug");
		}
		this.path = (String) configBody.get("path");
		if (configBody.get("headers") != null) {
			setHeaders((Map) configBody.get("headers"));
		}
		if (configBody.get("method") != null) {
			setMethod((String) configBody.get("method"));
		} else {
			setMethod("GET");
		}

		if (configBody.get("langDef") != null) {
			langDef = (Map) configBody.get("langDef");
		}
		if (configBody.get("bodyDef") != null) {
			bodyDef = (Map) configBody.get("bodyDef");
		}
        if (configBody.get("paramsDef") != null) {
            paramsDef = (Map) configBody.get("paramsDef");
        }
        if (configBody.get("headersDef") != null) {
            headersDef = (Map) configBody.get("headersDef");
        }
        if (configBody.get("scriptValidate") != null) {
            scriptValidate = (Map) configBody.get("scriptValidate");
        }
        if (configBody.get("validateResponse") != null) {
            validateResponse = (Map) configBody.get("validateResponse");
        }
        if (configBody.get("contentType") != null) {
        	contentType = (String) configBody.get("contentType");
        }
        if (configBody.get("xmlArrPaths") != null) {
        	xmlArrPaths = (String) configBody.get("xmlArrPaths");
        }
	}

	public ClientInputConfig() {
        super(null);
    }


	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Map<String, Object> getLangDef() {
		return langDef;
	}

	public void setLangDef(Map<String, Object> langDef) {
		this.langDef = langDef;
	}

	public Map<String, Object> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, Object> headers) {
		this.headers = headers;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Map<String, Object> getBodyDef() {
		return bodyDef;
	}

	public void setBodyDef(Map<String, Object> bodyDef) {
		this.bodyDef = bodyDef;
	}

    public Map<String, Object> getHeadersDef() {
        return headersDef;
    }

    public void setHeadersDef(Map<String, Object> headersDef) {
        this.headersDef = headersDef;
    }

    public Map<String, Object> getParamsDef() {
        return paramsDef;
    }

    public void setParamsDef(Map<String, Object> paramsDef) {
        this.paramsDef = paramsDef;
    }

    public Map<String, Object> getScriptValidate() {
        return scriptValidate;
    }

    public void setScriptValidate(Map<String, Object> scriptValidate) {
        this.scriptValidate = scriptValidate;
    }

    public Map<String, Object> getValidateResponse() {
        return validateResponse;
    }

    public void setValidateResponse(Map<String, Object> validateResponse) {
        this.validateResponse = validateResponse;
    }

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getXmlArrPaths() {
		return xmlArrPaths;
	}

	public void setXmlArrPaths(String xmlArrPaths) {
		this.xmlArrPaths = xmlArrPaths;
	}
    
}
