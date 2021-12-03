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

import we.dedicatedline.proxy.codec.FizzTcpTextMessage;
import we.dedicatedline.proxy.codec.FizzUdpTextMessage;
import we.util.Consts;
import we.util.JacksonUtils;

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

	private boolean leftIn;
	private boolean rightOut;
	private boolean rightIn;
	private boolean leftOut;

	private int tcpMessageMaxLength;
	private int udpMessageMaxLength;

	private String logMsg;

	public ProxyConfig(String protocol, Integer serverPort, String targetHost, Integer targetPort, int maxIdleInSec, boolean leftIn, boolean rightOut, boolean rightIn, boolean leftOut,
					   int tcpMessageMaxLength, int udpMessageMaxLength) {
		this.protocol = protocol;
		this.serverPort = serverPort;
		this.targetHost = targetHost;
		this.targetPort = targetPort;
		this.maxIdleInSec = maxIdleInSec;
		this.leftIn = leftIn;
		this.rightOut = rightOut;
		this.rightIn = rightIn;
		this.leftOut = leftOut;
		this.tcpMessageMaxLength = tcpMessageMaxLength > 0 ? tcpMessageMaxLength : FizzTcpTextMessage.MAX_LENGTH;
		this.udpMessageMaxLength = udpMessageMaxLength > 0 ? udpMessageMaxLength : FizzUdpTextMessage.MAX_LENGTH;
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

	public int getTcpMessageMaxLength() {
		return tcpMessageMaxLength;
	}

	public void setTcpMessageMaxLength(int tcpMessageMaxLength) {
		this.tcpMessageMaxLength = tcpMessageMaxLength > 0 ? tcpMessageMaxLength : FizzTcpTextMessage.MAX_LENGTH;
	}

	public int getUdpMessageMaxLength() {
		return udpMessageMaxLength;
	}

	public void setUdpMessageMaxLength(int udpMessageMaxLength) {
		this.udpMessageMaxLength = udpMessageMaxLength > 0 ? udpMessageMaxLength : FizzUdpTextMessage.MAX_LENGTH;
	}

	public String logMsg() {
		if (logMsg == null) {
			StringBuilder b = new StringBuilder();
			b.append(Consts.S.LEFT_SQUARE_BRACKET)
			 .append(serverPort).append(Consts.S.DASH).append(targetHost).append(Consts.S.COLON).append(targetPort)
			 .append(Consts.S.RIGHT_SQUARE_BRACKET);
			logMsg = b.toString();
		}
		return logMsg;
	}

	public String toString() {
		return JacksonUtils.writeValueAsString(this);
	}
}
