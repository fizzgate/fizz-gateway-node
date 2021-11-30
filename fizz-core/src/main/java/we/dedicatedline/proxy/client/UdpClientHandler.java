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
package we.dedicatedline.proxy.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.dedicatedline.proxy.ProxyConfig;
import we.dedicatedline.proxy.codec.FizzSocketMessage;
import we.dedicatedline.proxy.codec.FizzUdpMessage;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 
 * @author Francis Dong
 *
 */
public class UdpClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

	private static final Logger log = LoggerFactory.getLogger(UdpClientHandler.class);

	private final ChannelHandlerContext proxyServerChannelCtx;

	/**
	 * For UDP
	 */
	private final InetSocketAddress senderAddress;
	private final ProxyClient proxyClient;

	private ProxyConfig proxyConfig;

	public UdpClientHandler(InetSocketAddress senderAddress, ChannelHandlerContext proxyServerChannelCtx, ProxyClient proxyClient, ProxyConfig proxyConfig) {
		super(false);
		this.senderAddress = senderAddress;
		this.proxyServerChannelCtx = proxyServerChannelCtx;
		this.proxyClient = proxyClient;
		this.proxyConfig = proxyConfig;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
		if (log.isDebugEnabled()) {
			DatagramPacket copy = packet.copy();
			log.debug("udp client to {}:{} receive msg: {}", proxyConfig.getTargetHost(), proxyConfig.getTargetPort(), FizzUdpMessage.decode(copy));
		}

		if (proxyConfig.getRole().equals(ProxyConfig.SERVER)) {
			ByteBuf buf = packet.content();
			byte[] contentBytes = new byte[buf.readableBytes()];
			buf.readBytes(contentBytes);
			List<DatagramPacket> datagramPackets = FizzUdpMessage.disassemble(senderAddress, contentBytes);
			for (DatagramPacket datagramPacket : datagramPackets) {
				if (log.isDebugEnabled()) {
					DatagramPacket copy = datagramPacket.copy();
					log.debug("{} udp server {} response {}:{} client: {}", packet.hashCode(), proxyConfig.getServerPort(), senderAddress.getHostString(), senderAddress.getPort(), FizzUdpMessage.decode(copy));
				}
				proxyServerChannelCtx.writeAndFlush(datagramPacket);
			}

		} else {
			ByteBuf content = packet.content();
			content.skipBytes(FizzSocketMessage.METADATA_LENGTH);
			byte[] bytes = new byte[content.readableBytes()];
			content.readBytes(bytes);
			FizzSocketMessage.inv(bytes);
			ByteBuf buf = Unpooled.copiedBuffer(bytes);
			proxyServerChannelCtx.writeAndFlush(new DatagramPacket(buf, senderAddress));
			if (log.isDebugEnabled()) {
				log.debug("udp server {} response {}:{} client: {}", proxyConfig.getServerPort(), senderAddress.getHostString(), senderAddress.getPort(), new String(bytes));
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.error("异常:", cause);
		proxyClient.remove();
		proxyClient.disconnect();
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		super.channelRegistered(ctx);
		log.info("client channelRegistered, channelId={}", ctx.channel().id().asLongText());
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		super.channelUnregistered(ctx);
		log.info("client channelUnregistered, channelId={}", ctx.channel().id().asLongText());
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.ALL_IDLE) {
				processAllIdle(ctx);
				return;
			}
		}
		super.userEventTriggered(ctx, evt);
	}

	private void processAllIdle(ChannelHandlerContext ctx) {
		String channelId = ctx.channel().id().asLongText();
		proxyClient.remove();
		proxyClient.disconnect();
		log.debug("[Netty]connection(id=" + channelId + ") reached max idle time, connection closed.");
	}

}
