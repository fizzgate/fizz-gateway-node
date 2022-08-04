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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author hongqiaowei
 */

public abstract class ReflectionUtils extends org.springframework.util.ReflectionUtils {

    private ReflectionUtils() {
    }

    public static void set(Object target, String field, Object value) {
        Field f = findField(target.getClass(), field);
        makeAccessible(f);
        setField(f, target, value);
    }

    public static Object get(Object target, String field) {
        Field f = findField(target.getClass(), field);
        makeAccessible(f);
        return getField(f, target);
    }

    public static Object invokeMethod(String method, Object target) {
        return invokeMethod(method, target, null, null);
    }

    public static Object invokeMethod(String method, Object target, Class<?>[] argTypes, Object[] args) {
        Method m = null;
        if (args == null) {
            m = findMethod(target.getClass(), method);
        } else {
            m = findMethod(target.getClass(), method, argTypes);
        }
        makeAccessible(m);
        return invokeMethod(m, target, args);
    }
}
