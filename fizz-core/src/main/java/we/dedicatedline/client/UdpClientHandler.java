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
package we.dedicatedline.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * 
 * @author Francis Dong
 *
 */
public class UdpClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

	private static final Logger log = LoggerFactory.getLogger(UdpClientHandler.class);

	private ChannelHandlerContext proxyServerChannelCtx;

	/**
	 * For UDP
	 */
	private InetSocketAddress senderAddress;

	public UdpClientHandler(InetSocketAddress senderAddress, ChannelHandlerContext proxyServerChannelCtx) {
		super(false);
		this.senderAddress = senderAddress;
		this.proxyServerChannelCtx = proxyServerChannelCtx;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
		log.info("UDP client channel read......");
		proxyServerChannelCtx.writeAndFlush(new DatagramPacket(packet.content(), senderAddress));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error("异常:", cause);
		ctx.close();
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		super.channelRegistered(ctx);
		log.info("client channelRegistered, channelId={}", ctx.channel().id().asLongText());
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		this.proxyServerChannelCtx.close();
		super.channelUnregistered(ctx);
		log.info("client channelUnregistered, channelId={}", ctx.channel().id().asLongText());
	}

}
