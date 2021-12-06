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
package we.dedicatedline.proxy.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.dedicatedline.DedicatedLineUtils;
import we.dedicatedline.proxy.ProxyConfig;
import we.dedicatedline.proxy.client.ProxyClient;
import we.dedicatedline.proxy.codec.FizzSocketTextMessage;
import we.dedicatedline.proxy.codec.FizzUdpTextMessage;
import we.dedicatedline.proxy.codec.FizzUdpTextMessageCodec;
import we.util.NettyByteBufUtils;

import java.net.InetSocketAddress;

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

		InetSocketAddress sender = packet.sender();
		ProxyClient proxyClient = this.channelManager.getClient(sender.toString(), sender, this.proxyConfig, ctx);

		if (proxyConfig.isLeftIn()) {
			FizzUdpTextMessage msg = FizzUdpTextMessageCodec.decode(packet, proxyConfig, ProxyConfig.LEFT_IN);
			String dedicatedLine = msg.getDedicatedLineStr();
			long timestamp = msg.getTimestamp();
			String sign = msg.getSignStr();
			String sign0 = DedicatedLineUtils.sign(dedicatedLine, timestamp, "ade052c1ec3e44a3bbfbaac988a6e7d4");
			if (!sign0.substring(0, FizzSocketTextMessage.SIGN_LENGTH).equals(sign)) {
				byte[] bytes = "sign invalid".getBytes();
				if (proxyConfig.isLeftOut()) {
					msg.setContent(bytes);
					DatagramPacket enc = FizzUdpTextMessageCodec.encode(msg, sender, proxyConfig, ProxyConfig.LEFT_OUT);
					ctx.writeAndFlush(enc);
				} else {
					ByteBuf buf = Unpooled.wrappedBuffer(bytes);
					DatagramPacket pk = new DatagramPacket(buf, sender);
					ctx.writeAndFlush(pk);
					if (log.isDebugEnabled()) {
						log.debug("{} {}: {}", proxyConfig.logMsg(), ProxyConfig.LEFT_OUT, new String(bytes));
					}
				}
				return;
			}

			/*if (proxyConfig.getServerPort() == 6666) {
				msg.setContent("udp msg from 6666".getBytes());
				DatagramPacket encode = FizzUdpTextMessageCodec.encode(msg, sender, proxyConfig, "left out");
				ctx.writeAndFlush(encode);
				return;
			}*/

			proxyClient.write(msg);

		} else {

			if (log.isDebugEnabled()) {
				log.debug("{} {}: {}", proxyConfig.logMsg(), ProxyConfig.LEFT_IN, NettyByteBufUtils.toString(packet.content()));
			}

			/*if (proxyConfig.getServerPort() == 6666) {
				log.debug(proxyConfig.logMsg() + " left out " + " udp msg from 6666");
				byte[] bytes = "udp msg from 6666".getBytes();
				ByteBuf byteBuf = Unpooled.copiedBuffer(bytes);
				DatagramPacket packet1 = new DatagramPacket(byteBuf, sender);
				ctx.writeAndFlush(packet1);
				return;
			}*/

			proxyClient.write(packet.content());
		}
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

		this.channelManager.removeClient(channelId);
	}

}
