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
package we.dedicatedline.proxy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author Francis Dong
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyConfig {

	/**
	 * protocol, support: TCP/UDP
	 */
	private String protocol;
	private Integer serverPort;
	private String targetHost;
	private Integer targetPort;
	
	// max idle time in second, 0 for no limit
	private int maxIdleInSec;

	private String role;

	public static final String CLIENT = "client";
	public static final String SERVER = "server";

}
