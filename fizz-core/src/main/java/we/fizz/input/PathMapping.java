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

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.noear.snack.ONode;

import we.constants.CommonConstants;
import we.fizz.StepContext;
import we.fizz.exception.FizzRuntimeException;
import we.fizz.function.FuncExecutor;
import we.fizz.function.IFunc;
import we.global_resource.GlobalResourceService;
import we.util.MapUtil;

/**
 * 
 * @author Francis Dong
 *
 */
public class PathMapping {
	
	private static final String GLOBAL_RESOURCE_PREFIX = "g.";

	private static List<String> typeList = Arrays.asList("Integer", "int", "Boolean", "boolean", "Float", "float",
			"Double", "double", "String", "string", "Long", "long", "Number", "number");
	
	public static ONode toONode(Object obj) {
		ONode o = null;
		synchronized (obj) {
			o = ONode.loadObj(obj);
		}
		return o;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void setByPath(ONode target, String path, Object obj, boolean supportMultiLevels) {
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
			if (!supportMultiLevels) {
				keys = new String[] { path };
			}
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

	public static Map<String, Object> transformToMap(ONode ctxNode, Map<String, Object> rules, boolean supportMultiLevels) {
		ONode target = transform(ctxNode, rules, supportMultiLevels);
		return target.toObject(Map.class);
	}

	@SuppressWarnings("unchecked")
	public static ONode transform(ONode ctxNode, Map<String, Object> rules, boolean supportMultiLevels) {
		ONode target = ONode.load(new HashMap());
		if (rules.isEmpty()) {
			return target;
		}

		Map<String, Object> rs = new HashMap<>();
		Map<String, Object> types = new HashMap<>();
		for (Entry<String, Object> entry : rules.entrySet()) {
			if (entry.getValue() instanceof String) {
				String val = (String) entry.getValue();
				Optional<String> optType = typeList.stream().filter(s -> val.startsWith(s + " ")).findFirst();
				if (optType.isPresent()) {
					rs.put(entry.getKey(), val.substring(optType.get().length() + 1));
					types.put(entry.getKey(), optType.get());
				} else {
					rs.put(entry.getKey(), val);
				}
			} else if (entry.getValue() instanceof List) {
				List<Object> values = (List<Object>) entry.getValue();
				List<String> vList = new ArrayList<>();
				List<String> tList = new ArrayList<>();
				for (Object v : values) {
					if (v instanceof String) {
						String val = (String) v;
						Optional<String> optType = typeList.stream().filter(s -> val.startsWith(s + " ")).findFirst();
						if (optType.isPresent()) {
							vList.add(val.substring(optType.get().length() + 1));
							tList.add(optType.get());
						} else {
							vList.add(val);
							tList.add(null);
						}
					}
				}
				rs.put(entry.getKey(), vList);
				types.put(entry.getKey(), tList);
			}
		}

		if (rs.isEmpty()) {
			return target;
		}

		// wildcard star entry 
		Object starValObj = null;
		String starEntryKey = null;
		
		for (Entry<String, Object> entry : rs.entrySet()) {
			if (entry.getValue() instanceof String) {
				String path = (String) entry.getValue();
				String type = (String) types.get(entry.getKey());
				Object obj = getRefValue(ctxNode, type, path);
				if (CommonConstants.WILDCARD_STAR.equals(entry.getKey())) {
					starValObj = obj;
					starEntryKey = entry.getKey();
				} else {
					setByPath(target, entry.getKey(), obj, supportMultiLevels);
				}
			} else if (entry.getValue() instanceof List) {
				List<String> refs = (List<String>) entry.getValue();
				List<String> tList = (List<String>) types.get(entry.getKey());
				List<Object> refValList = new ArrayList<>();
				for (int i = 0; i < refs.size(); i++) {
					String path = refs.get(i);
					String type = tList.get(i);
					Object obj = getRefValue(ctxNode, type, path);
					// Only header form-data and query Parameter support multiple values, merge result into
					// one a list
					if (obj instanceof List) {
						refValList.addAll((List<Object>) obj);
					} else {
						refValList.add(obj);
					}
				}
				setByPath(target, entry.getKey(), refValList, supportMultiLevels);
			}
		}
		
		if(starEntryKey != null) {
			setByPath(target, starEntryKey, starValObj, supportMultiLevels);
		}
		return target;
	}
	
	private static Object getRefValue(ONode ctxNode, String type, String path) {
		if (StringUtils.isBlank(path)) {
			return null;
		}
		Object obj = null;
		// check if it is a function
		if (path.startsWith(IFunc.NAME_SPACE_PREFIX)) {
			obj = FuncExecutor.getInstance().exec(ctxNode, path);
			if (obj != null && type != null) {
				obj = cast(obj, type, path);
			}
		} else {
			try {
				String p = path;
				String defaultValue = null;
				if (path.indexOf("|") != -1) {
					p = path.substring(0, path.indexOf("|"));
					defaultValue = path.substring(path.indexOf("|") + 1);
				}
				ONode val = null;
				if (path.startsWith(GLOBAL_RESOURCE_PREFIX)) {
					val = select(GlobalResourceService.resNode, p);
				} else {
					val = select(ctxNode, handlePath(p));
				}
				if (val != null && !val.isNull()) {
					obj = val;
				} else {
					obj = defaultValue;
				}
				if (obj != null && type != null) {
					obj = cast(obj, type, path);
				}
			} catch (Exception e) {
				// e.printStackTrace();
				throw new FizzRuntimeException(String.format("path mapping errer: %s , path mapping data: %s %s", e.getMessage(), type, path), e);
			}
		}
		return obj;
	}
	
	private static Object cast(Object obj, String type, String path) {
		try {
			switch (type) {
			case "Integer":
			case "int": {
				if (obj instanceof ONode) {
					obj = ((ONode) obj).val().getInt();
				} else {
					obj = Integer.valueOf(obj.toString());
				}
				break;
			}
			case "Boolean":
			case "boolean": {
				if (obj instanceof ONode) {
					obj = ((ONode) obj).val().getBoolean();
				} else {
					obj = Boolean.valueOf(obj.toString());
				}
				break;
			}
			case "Float":
			case "float": {
				if (obj instanceof ONode) {
					obj = ((ONode) obj).val().getFloat();
				} else {
					obj = Float.valueOf(obj.toString());
				}
				break;
			}
			case "Double":
			case "double": {
				if (obj instanceof ONode) {
					obj = ((ONode) obj).val().getDouble();
				} else {
					obj = Double.valueOf(obj.toString());
				}
				break;
			}
			case "String":
			case "string": {
				if (obj instanceof ONode) {
					obj = ((ONode) obj).val().getString();
				} else {
					obj = String.valueOf(obj.toString());
				}
				break;
			}
			case "Long":
			case "long": {
				if (obj instanceof ONode) {
					obj = ((ONode) obj).val().getLong();
				} else {
					obj = Long.valueOf(obj.toString());
				}
				break;
			}
			}
			return obj;
		} catch (Exception e) {
			// e.printStackTrace();
			throw new FizzRuntimeException(String.format("failed to cast %s to %s, JSON path expression: %s, error: %s", obj, type, path, e.getMessage()), e);
		}
	}
	
	public static ONode select(ONode ctxNode, String path) {
		ONode val = ctxNode.select("$." + path);
		if (val != null && !val.isNull()) {
			return val;
		}
		String[] arr = path.split("\\.");
		if (arr.length == 6 && "headers".equals(arr[4]) && arr[5].endsWith("[0]")) {
			ONode v = ctxNode.select("$." + path.substring(0, path.length() - 3));
			if (!v.isArray()) {
				return v;
			}
		}
		if (arr.length == 4 && "headers".equals(arr[2]) && arr[3].endsWith("[0]")) {
			ONode v = ctxNode.select("$." + path.substring(0, path.length() - 3));
			if (!v.isArray()) {
				return v;
			}
		}
		return val;
	}
	
	/**
	 * Returns value of path, return default value if no value matched by path
	 * 
	 * @param ctxNode
	 * @param path    e.g: step1.request1.headers.abc or
	 *                step1.request1.headers.abc|123 (default value separate by "|")
	 * @return
	 */
	public static Object getValueByPath(ONode ctxNode, String path) {
		if (StringUtils.isBlank(path)) {
			return null;
		}
		String p = path;
		String defaultValue = null;
		if (path.indexOf("|") != -1) {
			p = path.substring(0, path.indexOf("|"));
			defaultValue = path.substring(path.indexOf("|") + 1);
		}
		ONode val = null;
		if (path.startsWith(GLOBAL_RESOURCE_PREFIX)) {
			val = select(GlobalResourceService.resNode, p);
		} else {
			val = select(ctxNode, handlePath(p));
		}
		if (val != null && !val.isNull()) {
			return val.toData();
		}
		return defaultValue;
	}
	
	public static Map<String, Object> getScriptRules(Map<String, Object> rules) {
		if (rules.isEmpty()) {
			return new HashMap<>();
		}
		Map<String, Object> rs = new HashMap<>();
		for (Entry<String, Object> entry : rules.entrySet()) {
			if (entry.getValue() instanceof List) {
				List<Object> values = (List<Object>) entry.getValue();
				for (Object v : values) {
					if (!(v instanceof String)) {
						rs.put(entry.getKey(), v);
					} 
				}
			} else if (!(entry.getValue() instanceof String) && entry.getValue() instanceof Map) {
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
			
			// upper case header name
			if (list.size() > 5 && "headers".equals(list.get(4))) {
				String headerName = list.get(5).toUpperCase();
				list.set(5, headerName);
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
			// upper case header name
			if (list.size() > 3 && "headers".equals(list.get(2))) {
				String headerName = list.get(3).toUpperCase();
				list.set(3, headerName);
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
		return transform(ctxNode, stepContext, fixed, mappingRules, true);
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
			Map<String, Object> fixed, Map<String, Object> mappingRules, boolean supportMultiLevels) {
		try {
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
				result.putAll((Map<String, Object>) convertPath(fixed, supportMultiLevels));
			}
			if (mappingRules != null) {
				// 路径映射
				ONode target = transform(ctxNode, mappingRules, supportMultiLevels);
				// 脚本转换
				Map<String, Object> scriptRules = getScriptRules(mappingRules);
				Map<String, Object> scriptResult = ScriptHelper.executeScripts(target, scriptRules, ctxNode, stepContext, supportMultiLevels);
				if (scriptResult != null && !scriptResult.isEmpty()) {
					result = MapUtil.merge(result, scriptResult);
				}
			}
			return result;
		}catch(FizzRuntimeException e) {
			throw new FizzRuntimeException(e.getMessage(), e, stepContext);
		}
	}
	
	public static Map<String, Object> convertPath(Map<String, Object> fixed, boolean supportMultiLevels) {
		ONode target = ONode.load(new HashMap());
		if (fixed.isEmpty()) {
			return target.toObject(Map.class);
		}

		// wildcard star entry 
		Object starValObj = null;
		String starEntryKey = null;
		
		for (Entry<String, Object> entry : fixed.entrySet()) {
			if (CommonConstants.WILDCARD_STAR.equals(entry.getKey())) {
				starValObj = entry.getValue();
				starEntryKey = entry.getKey();
			}else {
				setByPath(target, entry.getKey(), entry.getValue(), supportMultiLevels);
			}
		}
		if(starEntryKey != null) {
			setByPath(target, starEntryKey, starValObj, supportMultiLevels);
		}

		return target.toObject(Map.class);
	}
}
