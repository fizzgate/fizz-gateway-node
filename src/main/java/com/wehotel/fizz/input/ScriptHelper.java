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

package com.wehotel.fizz.input;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptException;

import org.noear.snack.ONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.wehotel.constants.CommonConstants;
import com.wehotel.exception.StopAndResponseException;
import com.wehotel.fizz.StepContext;
import com.wehotel.util.Script;
import com.wehotel.util.ScriptUtils;

/**
 * 
 * @author francis
 *
 */
public class ScriptHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptHelper.class);

	public static Object execute(Map<String, Object> scriptCfg, ONode ctxNode, StepContext<String, Object> stepContext)
			throws ScriptException {
		return execute(scriptCfg, ctxNode, stepContext, Object.class);
	}

	@SuppressWarnings("unchecked")
	public static <T> T execute(Map<String, Object> scriptCfg, ONode ctxNode, StepContext<String, Object> stepContext, Class<T> clazz)
			throws ScriptException {
		Script script = new Script();
		script.setType((String) scriptCfg.get("type"));
		script.setSource((String) scriptCfg.get("source"));
		if (StringUtils.isBlank(script.getType()) || StringUtils.isBlank(script.getSource())) {
			return null;
		}

		Map<String, Object> ctx = new HashMap<>();
		ctx.put("context", stepContext);

		Object rs = ScriptUtils.execute(script, ctx);
		if (ScriptUtils.GROOVY.equals(script.getType())) {
			return (T) handleStopResponse(stepContext, rs);
		} else if (ScriptUtils.JAVA_SCRIPT.equals(script.getType())) {
			if(rs != null) {
				if(rs instanceof Collection || rs instanceof Map) {
					return (T) rs;
				}else {
					String json = rs.toString();
					if(json.startsWith("[") && json.endsWith("]")) {
						return JSON.parseArray(json).toJavaObject(clazz);
					}else if(json.startsWith("{") && json.endsWith("}")) {
						if(clazz.isAssignableFrom(Map.class)) {
							return (T)handleStopResponse(stepContext, JSON.parseObject(json).toJavaObject(clazz));
						}else {
							handleStopResponse(stepContext, JSON.parseObject(json).toJavaObject(Map.class));
							return JSON.parseObject(json).toJavaObject(clazz);
						}
					}
				}
				return (T) rs;
			}
			return null;
		} else {
			return (T) rs;
		}
	}

	public static Map<String, Object> executeScripts(ONode target, Map<String, Object> scriptRules, ONode ctxNode, 
			StepContext<String, Object> stepContext) {
		return executeScripts(target, scriptRules, ctxNode, stepContext, Object.class);
	}

	@SuppressWarnings("unchecked")
	public static <T> Map<String, T> executeScripts(ONode target, Map<String, Object> scriptRules, ONode ctxNode, 
			StepContext<String, Object> stepContext, Class<T> clazz) {
		if(target == null) {
			target = ONode.load(new HashMap());
		}
		if (scriptRules != null && !scriptRules.isEmpty()) {
			// wildcard star entry 
			Object starValObj = null;
			String starEntryKey = null;
			for (Entry<String, Object> entry : scriptRules.entrySet()) {
				Map<String, Object> scriptCfg = (Map<String, Object>) entry.getValue();
				try {
					if (CommonConstants.WILDCARD_STAR.equals(entry.getKey())) {
						starValObj = execute(scriptCfg, ctxNode, stepContext, clazz);
						starEntryKey = entry.getKey();
					}else {
						PathMapping.setByPath(target, entry.getKey(), execute(scriptCfg, ctxNode, stepContext, clazz));
					}
				} catch (ScriptException e) {
					LOGGER.warn("execute script failed, {}", e);
					throw new RuntimeException("execute script failed");
				}
			}
			if(starEntryKey != null) {
				PathMapping.setByPath(target, starEntryKey, starValObj);
			}
		}
		return target.toObject(Map.class);
	}
	
	public static Object handleStopResponse(StepContext<String, Object> stepContext, Object result) {
		if(result instanceof Map) {
			Map<String, Object> rs = (Map<String, Object>) result;
			if (rs.containsKey(CommonConstants.STOP_AND_RESPONSE_KEY)) {
				if (rs.get(CommonConstants.STOP_AND_RESPONSE_KEY) != null
						&& rs.get(CommonConstants.STOP_AND_RESPONSE_KEY) instanceof Boolean
						&& (Boolean) rs.get(CommonConstants.STOP_AND_RESPONSE_KEY)) {
					rs.remove(CommonConstants.STOP_AND_RESPONSE_KEY);
					// 测试模式返回StepContext
					if (stepContext.returnContext()) {
						rs.put("_context", stepContext);
					}
					throw new StopAndResponseException("stop and response", JSON.toJSONString(rs));
				} else {
					rs.remove(CommonConstants.STOP_AND_RESPONSE_KEY);
				}
			}
		}
		return result;
	}

}
