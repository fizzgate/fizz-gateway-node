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

package com.fizzgate.plugin.basicAuth;

import org.junit.jupiter.api.Test;

import com.fizzgate.plugin.basicAuth.BasicAuthPluginFilter;
import com.fizzgate.plugin.basicAuth.GlobalConfig;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 
 * @author Francis Dong
 *
 */
public class BasicAuthPluginFilterTests {

	@Test
	public void testCheckAuth() {

		GlobalConfig globalConfig = new GlobalConfig();

		Map<String, String> users = new HashMap<>();
		globalConfig.setUsers(users);

		users.put("abc", "123456");
		
		Map<String, String> routeUsers = new HashMap<>();
		routeUsers.put("abc", "123456");

		BasicAuthPluginFilter plugin = new BasicAuthPluginFilter();

		String authorization = "Basic " + Base64.getEncoder().encodeToString("a:b".getBytes());
		boolean result = plugin.checkAuth(authorization, globalConfig, routeUsers);
		assertFalse(result);

		authorization = "Basic " + Base64.getEncoder().encodeToString("a:".getBytes());
		result = plugin.checkAuth(authorization, globalConfig, routeUsers);
		assertFalse(result);

		authorization = "Basic " + Base64.getEncoder().encodeToString(":b".getBytes());
		result = plugin.checkAuth(authorization, globalConfig, routeUsers);
		assertFalse(result);

		authorization = "Basic " + Base64.getEncoder().encodeToString(":".getBytes());
		result = plugin.checkAuth(authorization, globalConfig, routeUsers);
		assertFalse(result);

		authorization = "Basic " + Base64.getEncoder().encodeToString("".getBytes());
		result = plugin.checkAuth(authorization, globalConfig, routeUsers);
		assertFalse(result);

		authorization = "";
		result = plugin.checkAuth(authorization, globalConfig, routeUsers);
		assertFalse(result);

		authorization = null;
		result = plugin.checkAuth(authorization, globalConfig, routeUsers);
		assertFalse(result);

		authorization = "Basic " + Base64.getEncoder().encodeToString("abc:123456".getBytes());
		result = plugin.checkAuth(authorization, globalConfig, routeUsers);
		assertTrue(result);

		authorization = "Basic" + Base64.getEncoder().encodeToString("abc:123456".getBytes());
		result = plugin.checkAuth(authorization, globalConfig, routeUsers);
		assertFalse(result);

		authorization = Base64.getEncoder().encodeToString("abc:123456".getBytes());
		result = plugin.checkAuth(authorization, globalConfig, routeUsers);
		assertFalse(result);
	}
}
