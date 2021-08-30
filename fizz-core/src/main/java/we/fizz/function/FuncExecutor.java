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
package we.fizz.function;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang3.StringUtils;
import org.noear.snack.ONode;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import we.fizz.exception.FizzRuntimeException;
import we.fizz.input.Input;
import we.fizz.input.PathMapping;

/**
 * Function Register
 * 
 * @author Francis Dong
 *
 */
public class FuncExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(FuncExecutor.class);

	private static final Map<String, IFunc> funcMap = new HashMap<>();

	private static Pattern NUMBER_PATTERN = Pattern
			.compile("^[-\\+]?[\\d]+\\s*[,\\)]{1}|^[-\\+]?[\\d]+\\.[\\d]+\\s*[,\\)]{1}");

	private static FuncExecutor singleton;

	public static FuncExecutor getInstance() {
		if (singleton == null) {
			synchronized (FuncExecutor.class) {
				if (singleton == null) {
					singleton = new FuncExecutor();
					init();
				}
			}
		}
		return singleton;
	}

	private FuncExecutor() {
	}

	public static void init() {
		try {
			Reflections reflections = new Reflections("we.fizz.function");
			Set<Class<? extends IFunc>> types = reflections.getSubTypesOf(IFunc.class);
			for (Class<? extends IFunc> fnType : types) {
				Method method = fnType.getMethod("getInstance");
				method.invoke(fnType);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Register a function instance
	 * 
	 * @param namespace a name to identify the given function instance
	 * @param func
	 */
	public static void register(String namespace, IFunc funcInstance) {
		if (StringUtils.isBlank(namespace)) {
			LOGGER.warn("namespace is required");
			return;
		}
		if (!namespace.startsWith(IFunc.NAME_SPACE_PREFIX)) {
			LOGGER.warn("namespace must start with fn.");
			return;
		}
		if (funcInstance == null) {
			LOGGER.warn("function instance is required");
			return;
		}
		funcMap.put(namespace, funcInstance);
	}

	/**
	 * Execute function
	 * 
	 * @param funcExpression
	 * @return
	 */
	public Object exec(ONode ctxNode, String funcExpression) {
		RecursionContext ctx = new RecursionContext();
		ctx.setFuncExpression(funcExpression);
		return doExec(ctxNode, ctx);
	}

	private Object doExec(ONode ctxNode, RecursionContext ctx) {
		String funcExpression = ctx.funcExpression;
		if (StringUtils.isBlank(funcExpression)) {
			return null;
		}
		funcExpression = StringUtils.trim(funcExpression);
		int pos1 = funcExpression.indexOf("(");
		if (pos1 == -1) {
			LOGGER.warn("func expression is invalid, expression: {}", funcExpression);
			return null;
		}
		if (!funcExpression.endsWith(")")) {
			LOGGER.warn("func expression is invalid, expression: {}", funcExpression);
			return null;
		}

		String path = funcExpression.substring(0, pos1);
		int lastDotPos = path.lastIndexOf(".");
		if (pos1 == -1) {
			LOGGER.warn("func expression is invalid, expression: {}", funcExpression);
			return null;
		}
		String namespace = path.substring(0, lastDotPos);
		String methodName = path.substring(lastDotPos + 1);

		Object funcInstance = funcMap.get(path);
		if (funcInstance == null) {
			String msg = String.format("function not found: %s, expression: %s", path, funcExpression);
			LOGGER.warn(msg);
			throw new FizzRuntimeException(msg);
		}

		try {
			Method method = findMethod(funcInstance.getClass(), methodName);
			Class[] paramTypes = method.getParameterTypes();
			ctx.funcExpression = funcExpression;
			Object[] args = parseArgs(ctxNode, ctx, funcExpression, paramTypes, method.isVarArgs());
			if (args == null) {
				return method.invoke(funcInstance);
			}
			return method.invoke(funcInstance, args);
		} catch (FizzRuntimeException e) {
			throw e;
		} catch (InvocationTargetException e) {
			Throwable targetEx = e.getTargetException();
			if (targetEx instanceof FizzRuntimeException) {
				throw (FizzRuntimeException) targetEx;
			}
			String msg = targetEx.getMessage();
			if (msg == null) {
				msg = String.format("execute function error: %s", funcExpression);
			}
			LOGGER.error(msg, targetEx);
			throw new FizzRuntimeException(msg, targetEx);
		} catch (Exception e) {
			String msg = String.format("execute function error: %s", funcExpression);
			LOGGER.error(msg, e);
			throw new FizzRuntimeException(msg, e);
		}
	}

	private Method findMethod(Class funcClass, String methodName) {
		Method[] methods = funcClass.getDeclaredMethods();
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				return method;
			}
		}
		String msg = String.format("method not found: %s, class: %s", methodName, funcClass);
		LOGGER.warn(msg);
		throw new FizzRuntimeException(msg);
	}

	/**
	 * funcExpression sample:<br>
	 * fn.date.add({step1.request1.response.body.date}, "yyyy-MM-dd HH:mm:ss", 1,
	 * 1000)<br>
	 * fn.date.add(true, fn.date.add({step1.request1.response.body.date},
	 * "yyyy-MM-dd HH:mm:ss", 1, 1000), "yyyy-MM-dd HH:mm:ss\"))}}", 1, 1000)<br>
	 * 
	 * @param funcExpression
	 * @param paramTypes
	 * @return
	 */
	private Object[] parseArgs(ONode ctxNode, RecursionContext ctx, String funcExpression, Class[] paramTypes,
			boolean isVarArgs) {
		int pos1 = funcExpression.indexOf("(");
		// int pos2 = funcExpression.lastIndexOf(")");
		String argsStr = funcExpression.substring(pos1 + 1);
		argsStr = StringUtils.trim(argsStr);
		// check if there is any argument
		if (StringUtils.isBlank(argsStr)) {
			if (paramTypes == null || paramTypes.length == 0) {
				return null;
			} else if (paramTypes.length == 1 && isVarArgs) {
				// check if variable arguments
				return null;
			} else {
				throw new FizzRuntimeException(
						String.format("missing argument, Function Expression: %s", funcExpression));
			}
		}
		Object[] args = new Object[paramTypes.length];
		List<Object> varArgs = new ArrayList<>();
		for (int i = 0; i < paramTypes.length; i++) {
			Class clazz = paramTypes[i];
			if (StringUtils.isBlank(argsStr)) {
				if (isVarArgs && i == paramTypes.length - 1 && args[i] == null) {
					args[i] = Array.newInstance(clazz.getComponentType(), 0);
				}
				break;
			}
			ArgsStrContainer argsStrContainer = new ArgsStrContainer(argsStr, i);
			if (argsStr.startsWith("\"")) { // string
				int pos = findStringEngPos(argsStr);
				if (pos != -1) {
					String arg = argsStr.substring(1, pos);
					if (isVarArgs && i == paramTypes.length - 1) {
						varArgs.add(arg);
						args[i] = varArgs.toArray(new String[varArgs.size()]);
					} else {
						args[i] = arg;
					}
					argsStrContainer = this.trimArgStr(argsStrContainer, pos + 1, isVarArgs, paramTypes.length,
							funcExpression);
					argsStr = argsStrContainer.getArgsStr();
					i = argsStrContainer.getIndex();
				} else {
					throw new FizzRuntimeException(
							String.format("invalid argument: %s, Function Expression: %s", argsStr, funcExpression));
				}
			} else if (argsStr.matches("^true\\s*,")) { // boolean
				if (isVarArgs && i == paramTypes.length - 1) {
					varArgs.add(true);
					args[i] = varArgs.toArray(new Boolean[varArgs.size()]);
				} else {
					args[i] = true;
				}
				argsStrContainer = this.trimArgStr(argsStrContainer, 4, isVarArgs, paramTypes.length, funcExpression);
				argsStr = argsStrContainer.getArgsStr();
				i = argsStrContainer.getIndex();
			} else if (argsStr.matches("^false\\s*,")) { // boolean
				if (isVarArgs && i == paramTypes.length - 1) {
					varArgs.add(false);
					args[i] = varArgs.toArray(new Boolean[varArgs.size()]);
				} else {
					args[i] = false;
				}
				argsStrContainer = this.trimArgStr(argsStrContainer, 5, isVarArgs, paramTypes.length, funcExpression);
				argsStr = argsStrContainer.getArgsStr();
				i = argsStrContainer.getIndex();
			} else if (argsStr.startsWith("{")) { // reference value
				int pos = argsStr.indexOf("}", 1);
				if (pos != -1) {
					String refKey = argsStr.substring(1, pos);
					Object arg = PathMapping.getValueByPath(ctxNode, refKey);
					arg = ConvertUtils.convert(arg, clazz.getComponentType());
					if (isVarArgs && i == paramTypes.length - 1) {
						varArgs.add(arg);
						Object arr = Array.newInstance(clazz.getComponentType(), varArgs.size());
						for (int j = 0; j < varArgs.size(); j++) {
							Array.set(arr, j, varArgs.get(j));
						}
						args[i] = arr;

					} else {
						args[i] = arg;
					}

					argsStrContainer = this.trimArgStr(argsStrContainer, pos + 1, isVarArgs, paramTypes.length,
							funcExpression);
					argsStr = argsStrContainer.getArgsStr();
					i = argsStrContainer.getIndex();
				} else {
					throw new FizzRuntimeException(
							String.format("invalid argument: %s, Function Expression: %s", argsStr, funcExpression));
				}
			} else {
				Matcher m = NUMBER_PATTERN.matcher(argsStr);
				boolean isNumber = m.find();
				if (isNumber) {
					int pos = m.end();
					String matchedStr = m.group();
					// Number
					String strNum = StringUtils.trim(matchedStr.substring(0, pos - 1));
					if (isVarArgs && i == paramTypes.length - 1) {
						Object arg = ConvertUtils.convert(strNum, clazz.getComponentType());
						varArgs.add(arg);
						Object arr = Array.newInstance(clazz.getComponentType(), varArgs.size());
						for (int j = 0; j < varArgs.size(); j++) {
							Array.set(arr, j, varArgs.get(j));
						}
						args[i] = arr;
					} else {
						Object arg = ConvertUtils.convert(strNum, clazz);
						args[i] = arg;
					}
					argsStrContainer = this.trimArgStr(argsStrContainer, pos - 1, isVarArgs, paramTypes.length,
							funcExpression);
					argsStr = argsStrContainer.getArgsStr();
					i = argsStrContainer.getIndex();
				} else {
					// function
					ctx.funcExpression = argsStr;
					Object rs = doExec(ctxNode, ctx);
					if (isVarArgs && i == paramTypes.length - 1) {
						Object arg = ConvertUtils.convert(rs, clazz.getComponentType());
						varArgs.add(arg);
						Object arr = Array.newInstance(clazz.getComponentType(), varArgs.size());
						for (int j = 0; j < varArgs.size(); j++) {
							Array.set(arr, j, varArgs.get(j));
						}
						args[i] = arr;
					} else {
						Object arg = ConvertUtils.convert(rs, clazz);
						args[i] = arg;
					}
					argsStr = ctx.funcExpression;
					argsStrContainer.setArgsStr(argsStr);
					argsStrContainer = this.trimArgStr(argsStrContainer, 0, isVarArgs, paramTypes.length,
							funcExpression);
					argsStr = argsStrContainer.getArgsStr();
					i = argsStrContainer.getIndex();
				}
			}
			ctx.funcExpression = argsStr;
		}
		return args;
	}

	private ArgsStrContainer trimArgStr(ArgsStrContainer argsStrContainer, int fromIndex, boolean isVarArgs,
			int paramTypesLen, String funcExpression) {
		int i = argsStrContainer.getIndex();
		String argsStr = argsStrContainer.getArgsStr();
		if (i == paramTypesLen - 1 || (isVarArgs && i == paramTypesLen - 2)) {
			boolean hasMore = hasMoreArg(argsStr, fromIndex);
			if (isVarArgs && hasMore) {
				argsStr = removeComma(argsStr, fromIndex, funcExpression);
				if (i == paramTypesLen - 1) {
					i--;
				}
			} else {
				if (hasCloseParenthesis(argsStr, fromIndex)) {
					argsStr = removeCloseParenthesis(argsStr, fromIndex, funcExpression);
				} else {
					throw new FizzRuntimeException(String.format("invalid argument: %s, Function Expression: %s",
							argsStr.substring(fromIndex), funcExpression));
				}
			}
		} else {
			argsStr = removeComma(argsStr, fromIndex, funcExpression);
		}
		argsStrContainer.setArgsStr(argsStr);
		argsStrContainer.setIndex(i);
		return argsStrContainer;
	}

	private boolean hasMoreArg(String argsStr, int fromIndex) {
		final int strLen = argsStr.length();
		if (strLen == 0) {
			return false;
		}
		for (int i = fromIndex; i < strLen; i++) {
			if (!Character.isWhitespace(argsStr.charAt(i))) {
				if (",".equals(String.valueOf(argsStr.charAt(i)))) {
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}

	private boolean hasCloseParenthesis(String argsStr, int fromIndex) {
		final int strLen = argsStr.length();
		if (strLen == 0) {
			return false;
		}
		for (int i = fromIndex; i < strLen; i++) {
			if (!Character.isWhitespace(argsStr.charAt(i))) {
				if (")".equals(String.valueOf(argsStr.charAt(i)))) {
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}

	private String removeComma(String argsStr, int fromIndex, String funcExpression) {
		final int strLen = argsStr.length();
		if (strLen == 0) {
			return argsStr;
		}
		for (int i = fromIndex; i < strLen; i++) {
			if (!Character.isWhitespace(argsStr.charAt(i))) {
				if (",".equals(String.valueOf(argsStr.charAt(i)))) {
					return StringUtils.trim(argsStr.substring(i + 1));
				}
			}
		}
		throw new FizzRuntimeException(String.format("missing comma after argument: %s, Function Expression: %s",
				argsStr.substring(fromIndex), funcExpression));
	}

	private String removeCloseParenthesis(String argsStr, int fromIndex, String funcExpression) {
		final int strLen = argsStr.length();
		if (strLen == 0 || strLen < fromIndex) {
			return argsStr;
		}
		for (int i = fromIndex; i < strLen; i++) {
			if (!Character.isWhitespace(argsStr.charAt(i))) {
				if (")".equals(String.valueOf(argsStr.charAt(i)))) {
					return StringUtils.trim(argsStr.substring(i + 1));
				}
			}
		}
		throw new FizzRuntimeException(
				String.format("missing close parenthesis after argument: %s, Function Expression: %s",
						argsStr.substring(fromIndex), funcExpression));
	}

	private int findStringEngPos(String ep) {
		int pos = ep.indexOf("\"", 1);
		while (pos != -1) {
			String prevChar = ep.substring(pos - 1, pos);
			if (!"\\".equals(prevChar)) {
				return pos;
			}
			pos = ep.indexOf("\"", pos);
		}
		return -1;
	}

}

@Data
@AllArgsConstructor
class ArgsStrContainer {
	private String argsStr;
	private int index;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class RecursionContext {
	public String funcExpression;
	public Object result;
}
