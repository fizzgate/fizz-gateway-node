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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.noear.snack.ONode;

import we.constants.CommonConstants;
import we.fizz.StepContext;

/**
 * 
 * @author francis
 *
 */
public class PathMapping {
	
	public static ONode toONode(Object obj) {
		ONode o = null;
		synchronized (obj) {
			o = ONode.loadObj(obj);
		}
		return o;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void setByPath(ONode target, String path, Object obj) {
		if (CommonConstants.WILDCARD_STAR.equals(path)) {
			if (obj instanceof ONode) {
				ONode node = (ONode) obj;
				if(node.isObject()) {
					target.setAll(node);
				}
			} else if (obj instanceof Map) {
				target.setAll((Map) obj);
			}
		} else {
			String[] keys = path.split("\\.");
			ONode cur = target;
			for (int i = 0; i < keys.length - 1; i++) {
				cur = cur.get(keys[i]);
			}
			
			if ((obj instanceof ONode && ((ONode) obj).isArray()) || obj instanceof Collection
					|| (obj instanceof ONode && ((ONode) obj).isObject()) || obj instanceof Map) {
				ONode subNode = cur.get(keys[keys.length - 1]);
				if ((obj instanceof ONode && ((ONode) obj).isArray()) || obj instanceof Collection) {
					if (subNode.isArray()) {
						if (obj instanceof ONode) {
							subNode.addAll((ONode) obj);
						} else if (obj instanceof Collection) {
							subNode.addAll((Collection) obj);
						}
					} else {
						subNode.fill(obj);
					}
				} else {
					if (subNode.isObject()) {
						if (obj instanceof ONode) {
							ONode node = (ONode) obj;
							if (node.isObject()) {
								subNode.setAll(node);
							}
						} else if (obj instanceof Map) {
							subNode.setAll((Map) obj);
						}
					} else {
						subNode.fill(obj);
					}
				}
			} else {
				cur.set(keys[keys.length - 1], obj);
			}
		}
	}

	public static Map<String, Object> transformToMap(ONode ctxNode, Map<String, Object> rules) {
		ONode target = transform(ctxNode, rules);
		return target.toObject(Map.class);
	}

	public static ONode transform(ONode ctxNode, Map<String, Object> rules) {
		ONode target = ONode.load(new HashMap());
		if (rules.isEmpty()) {
			return target;
		}

		Map<String, String> rs = new HashMap<>();
		Map<String, String> types = new HashMap<>();
		for (Entry<String, Object> entry : rules.entrySet()) {
			if (entry.getValue() instanceof String) {
				String val = (String) entry.getValue();
				String[] vals = val.split(" ");
				if (vals.length > 1) {
					rs.put(entry.getKey(), vals[1]);
					types.put(entry.getKey(), vals[0]);
				} else {
					rs.put(entry.getKey(), val);
				}
			}
		}

		if (rs.isEmpty()) {
			return target;
		}

		// wildcard star entry 
		Object starValObj = null;
		String starEntryKey = null;
		
		for (Entry<String, String> entry : rs.entrySet()) {
			ONode val = ctxNode.select("$." + handlePath(entry.getValue()));
			
			Object obj = val;
			if (val != null && types.containsKey(entry.getKey())) {
				switch (types.get(entry.getKey())) {
				case "Integer":
				case "int": {
					obj = val.val().isNull() ? null : val.val().getInt();
					break;
				}
				case "Boolean":
				case "boolean": {
					obj = val.val().isNull() ? null : val.val().getBoolean();
					break;
				}
				case "Float":
				case "float": {
					obj = val.val().isNull() ? null : val.val().getFloat();
					break;
				}
				case "Double":
				case "double": {
					obj = val.val().isNull() ? null : val.val().getDouble();
					break;
				}
				case "String": {
					obj = val.val().isNull() ? null : val.val().getString();
					break;
				}
				case "Long":
				case "long": {
					obj = val.val().isNull() ? null : val.val().getLong();
					break;
				}
				}
			}
			if (CommonConstants.WILDCARD_STAR.equals(entry.getKey())) {
				starValObj = obj;
				starEntryKey = entry.getKey();
			}else {
				setByPath(target, entry.getKey(), obj);
			}
		}
		
		if(starEntryKey != null) {
			setByPath(target, starEntryKey, starValObj);
		}

		return target;
	}
	
	public static Map<String, Object> getScriptRules(Map<String, Object> rules) {
		if (rules.isEmpty()) {
			return new HashMap<>();
		}
		Map<String, Object> rs = new HashMap<>();
		for (Entry<String, Object> entry : rules.entrySet()) {
			if (!(entry.getValue() instanceof String)) {
				rs.put(entry.getKey(), entry.getValue());
			}
		}
		return rs;
	}
	
	/**
	 * 把Path转为context里的实际路径<br/>
	 * 步骤兼容以下几种写法，把后几种转换为第一种标准路径<br/>
	 * 
	 * 例子1：<br/>
	 * step1.requests.request1.request.headers<br/>
	 * step1.request1.request.headers<br/>
	 * step1.request1.requestHeaders<br/>
	 * step1.requests.request1.requestHeaders<br/>
	 * 
	 * 例子2：<br/>
	 * step1.requests.request1.response.body<br/>
	 * step1.request1.response.body<br/>
	 * step1.request1.responseBody<br/>
	 * step1.requests.request1.responseBody<br/>
	 * 
	 * input兼容以下写法，把第二种转换为第一种标准路径<br/>
	 * 
	 * 例子1：<br/>
	 * input.request.headers<br/>
	 * input.requestHeaders<br/>
	 * 
	 * 例子2：<br/>
	 * input.response.body<br/>
	 * input.responseBody<br/>
	 * 
	 * @param path
	 * @return
	 */
	public static String handlePath(String path) {
		if(path.startsWith("step")) {
			String[] arr = path.split("\\.");
			
			List<String> list = Arrays.stream(arr).collect(Collectors.toList());
			// 补齐 requests
			// fix-如果是从step*.result下获取数据不应该插入requests
			if(list.size() >= 2 && !"requests".equals(list.get(1)) && !"result".equals(list.get(1))) {
				list.add(1,"requests");
			}
			
			// 拆分一级为两级，如：requestBody -> request.body
			if(list.size() >= 4) {
				String s = list.get(3);
				switch (s) {
				case "requestHeaders":
					list.set(3, "headers");
					list.add(3, "request");
					break;
				case "requestParams":
					list.set(3, "params");
					list.add(3, "request");
					break;
				case "requestBody":
					list.set(3, "body");
					list.add(3, "request");
					break;
				case "responseHeaders":
					list.set(3, "headers");
					list.add(3, "response");
					break;
				case "responseBody":
					list.set(3, "body");
					list.add(3, "response");
					break;
				}
			}
			return String.join(".", list);
		}else if(path.startsWith("input")) {
			String[] arr = path.split("\\.");
			
			List<String> list = Arrays.stream(arr).collect(Collectors.toList());
			
			// 拆分一级为两级，如：requestBody -> request.body
			if(list.size() >= 2) {
				String s = list.get(1);
				switch (s) {
				case "requestHeaders":
					list.set(1, "headers");
					list.add(1, "request");
					break;
				case "requestParams":
					list.set(1, "params");
					list.add(1, "request");
					break;
				case "requestBody":
					list.set(1, "body");
					list.add(1, "request");
					break;
				case "responseHeaders":
					list.set(1, "headers");
					list.add(1, "response");
					break;
				case "responseBody":
					list.set(1, "body");
					list.add(1, "response");
					break;
				}
			}
			return String.join(".", list);
			
		}else {
			return path;
		}
	}
	
	/**
	 * 数据转换
	 * 
	 * @param ctxNode
	 * @param stepContext
	 * @param fixed        optional
	 * @param mappingRules optional
	 * @return
	 */
	public static Map<String, Object> transform(ONode ctxNode, StepContext<String, Object> stepContext,
			Map<String, Object> fixed, Map<String, Object> mappingRules) {
		if (fixed != null && fixed.containsKey(CommonConstants.WILDCARD_TILDE)) {
			Object val = fixed.get(CommonConstants.WILDCARD_TILDE);
			fixed = new HashMap<>();
			fixed.put(CommonConstants.WILDCARD_TILDE, val);
		}
		if (mappingRules != null && mappingRules.containsKey(CommonConstants.WILDCARD_TILDE)) {
			Object val = mappingRules.get(CommonConstants.WILDCARD_TILDE);
			mappingRules = new HashMap<>();
			mappingRules.put(CommonConstants.WILDCARD_TILDE, val);
		}
		Map<String, Object> result = new HashMap<>();
		if (fixed != null) {
			result.putAll((Map<String, Object>) fixed);
		}
		if (mappingRules != null) {
			// 路径映射
			ONode target = PathMapping.transform(ctxNode, mappingRules);
			// 脚本转换
			Map<String, Object> scriptRules = PathMapping.getScriptRules(mappingRules);
			Map<String, Object> scriptResult = ScriptHelper.executeScripts(target, scriptRules, ctxNode, stepContext);
			if (scriptResult != null && !scriptResult.isEmpty()) {
				result.putAll(scriptResult);
			}
		}
		return result;
	}
	
}
