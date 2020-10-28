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

package we.util;

import org.apache.commons.lang3.StringUtils;

import javax.script.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

public abstract class ScriptUtils {

    public  static final String        JAVA_SCRIPT                       = "javascript";

    public  static final String        GROOVY                            = "groovy";

    private static ScriptEngineManager engineManger;

    private static final String        jsFuncName                        = "dyFunc";

    private static final String        clazz                             = "clazz";

    private static final String        resJsonStr                        = "resJsonStr";

    public static Map<Long, Long>      recreateJavascriptEngineSignalMap = new HashMap<>();

    static {
        engineManger = new ScriptEngineManager();
    }

    private static ScriptEngine createJavascriptEngine() throws ScriptException {
        ScriptEngine eng = engineManger.getEngineByName(JAVA_SCRIPT);
        try {
            eng.eval(new FileReader("js/common.js"));
            return eng;
        } catch (FileNotFoundException e) {
            throw new ScriptException(e);
        }
    }

    public static ScriptEngine getScriptEngine(String type) throws ScriptException {
        if (GROOVY.equals(type)) {
            ScriptEngine groovyEngine = (ScriptEngine) ThreadContext.get(GROOVY);
            if (groovyEngine == null) {
                groovyEngine = engineManger.getEngineByName(GROOVY);
                ThreadContext.set(GROOVY, groovyEngine);
            }
            return groovyEngine;

        } else if (JAVA_SCRIPT.equals(type)) {
            ScriptEngine javascriptEngine;
            long tid = Thread.currentThread().getId();
            Object signal = recreateJavascriptEngineSignalMap.get(tid);
            if (signal == null) {
                javascriptEngine = createJavascriptEngine();
                recreateJavascriptEngineSignalMap.put(tid, tid);
                ThreadContext.set(JAVA_SCRIPT, javascriptEngine);
            } else {
                javascriptEngine = (ScriptEngine) ThreadContext.get(JAVA_SCRIPT);
            }
            return javascriptEngine;

        } else {
            throw new ScriptException("unknown script engine type: " + type);
        }
    }

    public static Object execute(Script script) throws ScriptException {
        return execute(script, null);
    }

    public static Object execute(Script script, Map<String, Object> context) throws ScriptException {
        String type = script.getType();
        ScriptEngine engine = getScriptEngine(type);
        String src = script.getSource();
        if (GROOVY.equals(type)) {
            if (context == null) {
                return engine.eval(src);
            } else {
                Bindings bis = engine.createBindings();
                bis.putAll(context);
                return engine.eval(src, bis);
            }
        } else { // js
            engine.eval(src);
            Invocable invocable = (Invocable) engine;
            String paramsJsonStr = StringUtils.EMPTY;
            // try {
            //     ObjectMapper mapper = JacksonUtils.getObjectMapper();
            //     if (context != null) {
            //         paramsJsonStr = mapper.writeValueAsString(context);
            //     }
            //     ScriptObjectMirror som = (ScriptObjectMirror) invocable.invokeFunction(jsFuncName, paramsJsonStr);
            //     Class<?> clz = Class.forName(som.get(clazz).toString());
            //     return mapper.readValue(som.get(resJsonStr).toString(), clz);
            // } catch (JsonProcessingException | NoSuchMethodException | ClassNotFoundException e) {
            //     throw new ScriptException(e);
            // }
            try {
                // ObjectMapper mapper = JacksonUtils.getObjectMapper();
                if (context != null) {
                    paramsJsonStr = JacksonUtils.writeValueAsString(context);
                }
                return invocable.invokeFunction(jsFuncName, paramsJsonStr);
            } catch (NoSuchMethodException | RuntimeException e) {
                throw new ScriptException(e);
            }
        }
    }
}
