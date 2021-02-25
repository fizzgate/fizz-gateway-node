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

package we.fizz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import we.constants.CommonConstants;

/**
 * 
 * @author linwaiwai
 * @author francis
 *
 * @param <K>
 * @param <V>
 */
@SuppressWarnings("unchecked")
public class StepContext<K, V> extends ConcurrentHashMap<K, V> {

	public static final String ELAPSED_TIMES = "elapsedTimes";
	public static final String DEBUG = "debug";
	public static final String RETURN_CONTEXT = "returnContext";
	// context field in response body
	public static final String CONTEXT_FIELD = "_context";
	
	// exception info
	public static final String EXCEPTION_MESSAGE = "exceptionMessage";
	public static final String EXCEPTION_STACKS = "exceptionStacks";
	public static final String EXCEPTION_DATA = "exceptionData";
	
	public void setDebug(Boolean debug) {
		this.put((K)DEBUG, (V)debug);
	}
	
	public String getTraceId() {
		return (String) this.get(CommonConstants.TRACE_ID);
	}
	
	public void setTraceId(String traceId) {
		this.put((K)CommonConstants.TRACE_ID, (V)traceId);
	}

	/**
	 * 是否调试模式
	 * @return
	 */
	public Boolean isDebug() {
		return this.get(DEBUG) == null ? false : (Boolean) this.get(DEBUG);
	}

	/**
	 * 是否在响应体里返回上下文
	 * @return
	 */
	public boolean returnContext() {
		return Boolean.valueOf((String)getInputReqHeader(RETURN_CONTEXT));
	}
	
	/**
	 * set exception information
	 * @param cause exception
	 * @param exceptionData data that cause the exception, such as script source code, etc.
	 */
	public void setExceptionInfo(Throwable cause, Object exceptionData) {
		this.put((K) EXCEPTION_MESSAGE, (V) cause.getMessage());
		this.put((K) EXCEPTION_DATA, (V) exceptionData);

		StackTraceElement[] stacks = cause.getStackTrace();
		if (stacks != null && stacks.length > 0) {
			String[] arr = new String[stacks.length];
			for (int i = 0; i < stacks.length; i++) {
				StackTraceElement ste = stacks[i];
				StringBuffer sb = new StringBuffer();
				sb.append(ste.getClassName()).append(".").append(ste.getMethodName()).append("(")
						.append(ste.getFileName()).append(":").append(ste.getLineNumber()).append(")");
				arr[i] = sb.toString();
			}
			this.put((K) EXCEPTION_STACKS, (V) arr);
		}
	}
	
	public synchronized void addElapsedTime(String actionName, Long milliSeconds) {
		List<Map<String, Long>> elapsedTimes = (List<Map<String, Long>>) this.get(ELAPSED_TIMES);
		if (elapsedTimes == null) {
			elapsedTimes = new ArrayList<Map<String, Long>>();
			this.put((K) ELAPSED_TIMES, (V) elapsedTimes);
		}
		Map<String, Long> record = new HashMap<>();
		record.put(actionName, milliSeconds);
		elapsedTimes.add(record);
	}

	public V getElapsedTimes() {
		return this.get(ELAPSED_TIMES);
	}

	private Map<String, Object> getStepRequest(String stepName, String requestName) {
		StepResponse stepResponse = (StepResponse) this.get(stepName);
		if (stepResponse == null) {
			return null;
		}
		Map<String, Map<String, Object>> requests = (Map<String, Map<String, Object>>) stepResponse.getRequests();
		if (requests == null) {
			requests = new HashMap<>();
			stepResponse.setRequests(requests);
			requests.put(requestName, new HashMap<String, Object>());
		}
		return (Map<String, Object>) requests.get(requestName);
	}

	/**
	 * 设置Step里调用接口的请求头
	 * 
	 * @param stepName
	 * @param requestName
	 * @param headerName
	 * @param headerValue
	 */
	public void setStepReqHeader(String stepName, String requestName, String headerName, Object headerValue) {
		Map<String, Object> request = getStepRequest(stepName, requestName);
		if (request == null) {
			return;
		}
		Map<String, Object> req = (Map<String, Object>) request.get("request");
		if (req == null) {
			req = new HashMap<>();
			request.put("request", req);
		}
		Map<String, Object> headers = (Map<String, Object>) req.get("headers");
		if (headers == null) {
			headers = new HashMap<>();
			req.put("headers", headers);
		}
		headers.put(headerName, headerValue);
	}

	/**
	 * 获取Step里调用接口的请求头
	 * 
	 * @param stepName
	 * @param requestName
	 * @param headerName
	 */
	public Object getStepReqHeader(String stepName, String requestName, String headerName) {
		Map<String, Object> request = getStepRequest(stepName, requestName);
		if (request == null) {
			return null;
		}
		Map<String, Object> req = (Map<String, Object>) request.get("request");
		if (req == null) {
			return null;
		}
		Map<String, Object> headers = (Map<String, Object>) req.get("headers");
		if (headers == null) {
			return null;
		}
		return headers.get(headerName);
	}

	/**
	 * 设置Step里调用接口的请求body
	 * 
	 * @param stepName
	 * @param requestName
	 * @param fieldName
	 * @param value
	 */
	public void setStepReqBody(String stepName, String requestName, String key, Object value) {
		Map<String, Object> request = getStepRequest(stepName, requestName);
		if (request == null) {
			return;
		}
		Map<String, Object> req = (Map<String, Object>) request.get("request");
		if (req == null) {
			req = new HashMap<>();
			request.put("request", req);
		}
		Map<String, Object> body = (Map<String, Object>) req.get("body");
		if (body == null) {
			body = new HashMap<>();
			req.put("body", body);
		}
		body.put(key, value);
	}

	/**
	 * 获取Step里调用接口的请求body
	 * 
	 * @param stepName
	 * @param requestName
	 * @param fieldName 字段名
	 */
	public Object getStepReqBody(String stepName, String requestName, String fieldName) {
		Map<String, Object> request = getStepRequest(stepName, requestName);
		if (request == null) {
			return null;
		}
		Map<String, Object> req = (Map<String, Object>) request.get("request");
		if (req == null) {
			req = new HashMap<>();
			request.put("request", req);
		}
		Map<String, Object> body = (Map<String, Object>) req.get("body");
		if (body == null) {
			return null;
		}
		return body.get(fieldName);
	}

	/**
	 * 获取Step里调用接口的请求body
	 * 
	 * @param stepName
	 * @param requestName
	 */
	public Object getStepReqBody(String stepName, String requestName) {
		Map<String, Object> request = getStepRequest(stepName, requestName);
		if (request == null) {
			return null;
		}
		Map<String, Object> req = (Map<String, Object>) request.get("request");
		if (req == null) {
			req = new HashMap<>();
			request.put("request", req);
		}
		return req.get("body");
	}
	
	/**
	 * 获取Step里调用的接口的URL参数
	 * @param stepName 步骤名【必填】
	 * @param requestName 请求的接口名 【必填】
	 * @param paramName URL参数名 【选填】，不传时返回所有URL参数
	 */
	public Object getStepReqParam(String stepName, String requestName) {
		Map<String, Object> request = getStepRequest(stepName, requestName);
		if (request == null) {
			return null;
		}
		Map<String, Object> req = (Map<String, Object>) request.get("request");
		if (req == null) {
			req = new HashMap<>();
			request.put("request", req);
		}
		return req.get("params");
	}
	
	/**
	 * 获取Step里调用的接口的URL参数
	 * @param stepName 步骤名【必填】
	 * @param requestName 请求的接口名 【必填】
	 * @param paramName URL参数名 【必填】
	 */
	public Object getStepReqParam(String stepName, String requestName, String paramName) {
		Map<String, Object> params = (Map<String, Object>) this.getStepReqParam(stepName, requestName);
		return params == null ? null : params.get(paramName);
	}

	/**
	 * 设置Step里调用接口响应头
	 * 
	 * @param stepName
	 * @param requestName
	 * @param headerName
	 * @param headerValue
	 */
	public void setStepRespHeader(String stepName, String requestName, String headerName, Object headerValue) {
		Map<String, Object> request = getStepRequest(stepName, requestName);
		if (request == null) {
			return;
		}
		Map<String, Object> response = (Map<String, Object>) request.get("response");
		if (response == null) {
			response = new HashMap<>();
			request.put("response", response);
		}
		Map<String, Object> headers = (Map<String, Object>) response.get("headers");
		if (headers == null) {
			headers = new HashMap<>();
			response.put("headers", headers);
		}
		headers.put(headerName, headerValue);
	}

	/**
	 * 获取Step里调用接口响应头
	 * 
	 * @param stepName
	 * @param requestName
	 * @param headerName
	 */
	public Object getStepRespHeader(String stepName, String requestName, String headerName) {
		Map<String, Object> request = getStepRequest(stepName, requestName);
		if (request == null) {
			return null;
		}
		Map<String, Object> response = (Map<String, Object>) request.get("response");
		if (response == null) {
			return null;
		}
		Map<String, Object> headers = (Map<String, Object>) response.get("headers");
		if (headers == null) {
			return null;
		}
		return headers.get(headerName);
	}

	/**
	 * 设置Step里调用接口的响应body
	 * 
	 * @param stepName
	 * @param requestName
	 * @param key
	 * @param value
	 */
	public void setStepRespBody(String stepName, String requestName, String key, Object value) {
		Map<String, Object> request = getStepRequest(stepName, requestName);
		if (request == null) {
			return;
		}
		Map<String, Object> response = (Map<String, Object>) request.get("response");
		if (response == null) {
			response = new HashMap<>();
			request.put("response", response);
		}
		Map<String, Object> body = (Map<String, Object>) response.get("body");
		if (body == null) {
			body = new HashMap<>();
			response.put("body", body);
		}
		body.put(key, value);
	}

	/**
	 * 获取Step里调用接口的响应body
	 * 
	 * @param stepName
	 * @param requestName
	 * @param key
	 */
	public Object getStepRespBody(String stepName, String requestName, String key) {
		Map<String, Object> request = getStepRequest(stepName, requestName);
		if (request == null) {
			return null;
		}
		Map<String, Object> response = (Map<String, Object>) request.get("response");
		if (response == null) {
			return null;
		}
		Map<String, Object> body = (Map<String, Object>) response.get("body");
		if (body == null) {
			return null;
		}
		return body.get(key);
	}

	/**
	 * 获取Step里调用接口的响应body
	 * 
	 * @param stepName
	 * @param requestName
	 */
	public Object getStepRespBody(String stepName, String requestName) {
		Map<String, Object> request = getStepRequest(stepName, requestName);
		if (request == null) {
			return null;
		}
		Map<String, Object> response = (Map<String, Object>) request.get("response");
		if (response == null) {
			return null;
		}
		return response.get("body");
	}

	/**
	 * 设置Step的结果
	 * 
	 * @param stepName
	 * @param key
	 * @param value
	 */
	public void setStepResult(String stepName, String key, Object value) {
		StepResponse stepResponse = (StepResponse) this.get(stepName);
		if (stepResponse == null) {
			return;
		}
		Map<String, Object> result = (Map<String, Object>) stepResponse.getResult();
		if (result == null) {
			result = new HashMap<>();
			stepResponse.setResult(result);
		}
		result.put(key, value);
	}

	/**
	 * 获取Step的结果
	 * 
	 * @param stepName
	 * @param key
	 */
	public Object getStepResult(String stepName, String key) {
		StepResponse stepResponse = (StepResponse) this.get(stepName);
		if (stepResponse == null) {
			return null;
		}
		Map<String, Object> result = (Map<String, Object>) stepResponse.getResult();
		if (result == null) {
			return null;
		}
		return result.get(key);
	}

	/**
	 * 获取Step的结果
	 * 
	 * @param stepName
	 */
	public Object getStepResult(String stepName) {
		StepResponse stepResponse = (StepResponse) this.get(stepName);
		if (stepResponse == null) {
			return null;
		}
		return stepResponse.getResult();
	}

	/**
	 * 设置聚合接口的响应头
	 * 
	 * @param headerName
	 * @param headerValue
	 */
	public void setInputRespHeader(String headerName, Object headerValue) {
		Map<String, Object> input = (Map<String, Object>) this.get("input");
		if (input == null) {
			return;
		}
		Map<String, Object> response = (Map<String, Object>) input.get("response");
		if (response == null) {
			return;
		}
		Map<String, Object> headers = (Map<String, Object>) response.get("headers");
		if (headers == null) {
			headers = new HashMap<>();
			response.put("headers", headers);
		}
		headers.put(headerName, headerValue);
	}

	/**
	 * 获取聚合接口的响应头
	 * 
	 * @param headerName
	 */
	public Object getInputRespHeader(String headerName) {
		Map<String, Object> input = (Map<String, Object>) this.get("input");
		if (input == null) {
			return null;
		}
		Map<String, Object> response = (Map<String, Object>) input.get("response");
		if (response == null) {
			return null;
		}
		Map<String, Object> headers = (Map<String, Object>) response.get("headers");
		if (headers == null) {
			return null;
		}
		return headers.get(headerName);
	}

	/**
	 * 获取聚合接口的请求头
	 * 
	 * @param headerName
	 */
	public Object getInputReqHeader(String headerName) {
		Map<String, Object> input = (Map<String, Object>) this.get("input");
		if (input == null) {
			return null;
		}
		Map<String, Object> request = (Map<String, Object>) input.get("request");
		if (request == null) {
			return null;
		}
		Map<String, Object> headers = (Map<String, Object>) request.get("headers");
		if (headers == null) {
			return null;
		}
		return headers.get(headerName);
	}

	/**
	 * 设置聚合接口的响应body
	 * 
	 * @param fieldName
	 * @param value
	 */
	public void setInputRespBody(String fieldName, Object value) {
		Map<String, Object> input = (Map<String, Object>) this.get("input");
		if (input == null) {
			return;
		}
		Map<String, Object> response = (Map<String, Object>) input.get("response");
		if (response == null) {
			response = new HashMap<>();
			input.put("response", response);
		}
		Map<String, Object> body = (Map<String, Object>) response.get("body");
		if (body == null) {
			body = new HashMap<>();
			response.put("body", body);
		}
		body.put(fieldName, value);
	}

	/**
	 * 获取聚合接口的响应body
	 * 
	 * @param fieldName
	 */
	public Object getInputRespBody(String fieldName) {
		Map<String, Object> input = (Map<String, Object>) this.get("input");
		if (input == null) {
			return null;
		}
		Map<String, Object> response = (Map<String, Object>) input.get("response");
		if (response == null) {
			return null;
		}
		Map<String, Object> body = (Map<String, Object>) response.get("body");
		if (body == null) {
			return null;
		}
		return body.get(fieldName);
	}

	/**
	 * 获取聚合接口的响应body
	 * 
	 */
	public Object getInputRespBody() {
		Map<String, Object> input = (Map<String, Object>) this.get("input");
		if (input == null) {
			return null;
		}
		Map<String, Object> response = (Map<String, Object>) input.get("response");
		if (response == null) {
			return null;
		}
		return response.get("body");
	}

	/**
	 * 获取聚合接口的请求body
	 * 
	 * @param fieldName
	 */
	public Object getInputReqBody(String fieldName) {
		Map<String, Object> body = (Map<String, Object>) getInputReqAttr("body");
		if (body == null) {
			return null;
		}
		return body.get(fieldName);
	}

	/**
	 * 获取聚合接口的请求body
	 * 
	 */
	public Object getInputReqBody() {
		return getInputReqAttr("body");
	}
	
	/**
	 * 获取客户端URL请求参数（query string）
	 */
	public Object getInputReqParam() {
		return this.getInputReqAttr("params");
	}
	
	/**
	 * 获取客户端URL请求参数（query string）
	 * @param paramName URL参数名
	 */
	public Object getInputReqParam(String paramName) {
		Map<String, Object> params = (Map<String, Object>) this.getInputReqAttr("params");
		return params == null ? null : paramName == null ? params : params.get(paramName);
	}
	
	/**
	 * 获取聚合接口请求属性<br/>
	 * 可选属性：path,method,headers,params,body
	 * 
	 */
	public Object getInputReqAttr(String key) {
		Map<String, Object> input = (Map<String, Object>) this.get("input");
		if (input == null) {
			return null;
		}
		Map<String, Object> request = (Map<String, Object>) input.get("request");
		if (request == null) {
			return null;
		}
		return request.get(key);
	}

}
