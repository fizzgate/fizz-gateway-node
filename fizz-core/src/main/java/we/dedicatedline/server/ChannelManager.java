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
package we.dedicatedline.server;

import io.netty.channel.ChannelHandlerContext;
import we.dedicatedline.client.ProxyClient;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author Francis Dong
 *
 */
public class ChannelManager {

	private final Map<String, ProxyClient> channelMap;

	public ChannelManager() {
		channelMap = new ConcurrentHashMap<>();
	}

	public ProxyClient getClient(String key, InetSocketAddress senderAddress, String protocol, String host, Integer port,
								 ChannelHandlerContext proxyServerChannelCtx) {
		return channelMap.computeIfAbsent(key, k -> {
			ProxyClient proxyClient = new ProxyClient(key, senderAddress, protocol, host, port, proxyServerChannelCtx, this);
			proxyClient.connect();
			return proxyClient;
		});
	}

	public void removeClient(String key) {
		ProxyClient proxyClient = channelMap.remove(key);
		if (proxyClient != null) {
			proxyClient.disconnect();
		}
	}

	public void remove(String key, ProxyClient proxyClient) {
		channelMap.remove(key, proxyClient);
	}
}
