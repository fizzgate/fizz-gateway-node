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
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.dedicatedline.ProxyConfig;
import we.dedicatedline.client.ProxyClient;

import java.nio.charset.StandardCharsets;

/**
 * 
 * @author Francis Dong
 *
 */
public class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

	private static final Logger log = LoggerFactory.getLogger(UdpServerHandler.class);

	private ChannelManager channelManager;
	private ProxyConfig proxyConfig;

	public UdpServerHandler(ChannelManager channelManager, ProxyConfig proxyConfig) {
		super(false);
		this.channelManager = channelManager;
		this.proxyConfig = proxyConfig;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
		try {
			log.info("proxy channel read: {}", packet.content().toString(StandardCharsets.UTF_8));
		} catch (Exception e) {
			log.warn("udp read exception", e);
		}

		String sender = packet.sender().toString();
		ProxyClient proxyClient = this.channelManager.getChannelMap().get(sender);
		if (proxyClient == null || proxyClient.isClosed()) {
			proxyClient = new ProxyClient(packet.sender(), this.proxyConfig.getProtocol(),
					this.proxyConfig.getTargetHost(), this.proxyConfig.getTargetPort(), ctx);
			proxyClient.connect();
			this.channelManager.getChannelMap().put(sender, proxyClient);
		}
		proxyClient.write(packet);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error("exception:", cause);
		super.exceptionCaught(ctx, cause);
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		super.channelRegistered(ctx);
		log.info("proxy channelRegistered, channelId={}", ctx.channel().id().asLongText());
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		String channelId = ctx.channel().id().asLongText();
		super.channelUnregistered(ctx);
		log.info("proxy channelUnregistered, channelId={}", channelId);

		ProxyClient proxyClient = this.channelManager.getChannelMap().get(channelId);
		if (proxyClient != null) {
			proxyClient.disconnect();
		}
		this.channelManager.getChannelMap().remove(channelId);
	}

}
