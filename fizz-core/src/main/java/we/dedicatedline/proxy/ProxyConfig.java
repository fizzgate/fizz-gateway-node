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
import we.util.Consts;
import we.util.ThreadContext;

/**
 * 
 * @author Francis Dong
 *
 */
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
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

	private boolean leftIn;
	private boolean rightOut;
	private boolean rightIn;
	private boolean leftOut;

	public ProxyConfig(String protocol, Integer serverPort, String targetHost, Integer targetPort, int maxIdleInSec, String role, boolean leftIn, boolean rightOut, boolean rightIn, boolean leftOut) {
		this.protocol = protocol;
		this.serverPort = serverPort;
		this.targetHost = targetHost;
		this.targetPort = targetPort;
		this.maxIdleInSec = maxIdleInSec;
		this.role = role;
		this.leftIn = leftIn;
		this.rightOut = rightOut;
		this.rightIn = rightIn;
		this.leftOut = leftOut;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public Integer getServerPort() {
		return serverPort;
	}

	public void setServerPort(Integer serverPort) {
		this.serverPort = serverPort;
	}

	public String getTargetHost() {
		return targetHost;
	}

	public void setTargetHost(String targetHost) {
		this.targetHost = targetHost;
	}

	public Integer getTargetPort() {
		return targetPort;
	}

	public void setTargetPort(Integer targetPort) {
		this.targetPort = targetPort;
	}

	public int getMaxIdleInSec() {
		return maxIdleInSec;
	}

	public void setMaxIdleInSec(int maxIdleInSec) {
		this.maxIdleInSec = maxIdleInSec;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public boolean isLeftIn() {
		return leftIn;
	}

	public void setLeftIn(boolean leftIn) {
		this.leftIn = leftIn;
	}

	public boolean isRightOut() {
		return rightOut;
	}

	public void setRightOut(boolean rightOut) {
		this.rightOut = rightOut;
	}

	public boolean isRightIn() {
		return rightIn;
	}

	public void setRightIn(boolean rightIn) {
		this.rightIn = rightIn;
	}

	public boolean isLeftOut() {
		return leftOut;
	}

	public void setLeftOut(boolean leftOut) {
		this.leftOut = leftOut;
	}

	public static final String CLIENT = "client";
	public static final String SERVER = "server";

	public String getLogMsg() {
		StringBuilder b = new StringBuilder();
		b.append(Consts.S.LEFT_SQUARE_BRACKET)
		 .append(serverPort).append(Consts.S.DASH).append(targetHost).append(Consts.S.COLON).append(targetPort)
		 .append(Consts.S.RIGHT_SQUARE_BRACKET);
		return b.toString();
	}
}
