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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.dedicatedline.DedicatedLineUtils;
import we.dedicatedline.proxy.ProxyConfig;
import we.dedicatedline.proxy.client.ProxyClient;
import we.dedicatedline.proxy.codec.FizzSocketTextMessage;
import we.dedicatedline.proxy.codec.FizzTcpTextMessage;
import we.util.NettyByteBufUtils;

import java.util.Objects;

/**
 * 
 * @author Francis Dong
 *
 */
public class TcpServerHandler extends ChannelInboundHandlerAdapter {

	private static final Logger log = LoggerFactory.getLogger(TcpServerHandler.class);

	private ChannelManager channelManager;
	private ProxyConfig proxyConfig;

	public TcpServerHandler(ChannelManager channelManager, ProxyConfig proxyConfig) {
		this.channelManager = channelManager;
		this.proxyConfig = proxyConfig;
	}

	/**
	 * 客户端连接会触发
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.info("proxy channel active......");
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

		ProxyClient proxyClient = null;
//		if (proxyConfig.getServerPort() != 6666) {
			String channelId = ctx.channel().id().asLongText();
			proxyClient = this.channelManager.getClient(channelId, null, this.proxyConfig, ctx);
//		}

		FizzTcpTextMessage tcpTextMessage = null;
		try {
			if (proxyConfig.isLeftIn()) {
				tcpTextMessage = (FizzTcpTextMessage) msg;
				String dedicatedLine = tcpTextMessage.getDedicatedLineStr();
				long timestamp = tcpTextMessage.getTimestamp();
				String sign = tcpTextMessage.getSignStr();
				String sign0 = DedicatedLineUtils.sign(dedicatedLine, timestamp, "ade052c1ec3e44a3bbfbaac988a6e7d4");
				if (!sign0.substring(0, FizzSocketTextMessage.SIGN_LENGTH).equals(sign)) {
					String error = "sign invalid";
					log.warn("{} {}: {}, sign invalid", proxyConfig.logMsg(), ProxyConfig.LEFT_IN, tcpTextMessage);
					byte[] bytes = error.getBytes();
					tcpTextMessage.setContent(bytes);
					tcpTextMessage.setLength(bytes.length);
					if (proxyConfig.isLeftOut()) {
						ctx.writeAndFlush(tcpTextMessage);
					} else {
						ByteBuf buf = NettyByteBufUtils.toByteBuf(bytes);
						ctx.writeAndFlush(buf);
					}
					return;
				}

				/*if (proxyConfig.getServerPort() == 6666) {
					byte[] content = "tcp msg from 6666".getBytes();
					tcpTextMessage.setContent(content);
					tcpTextMessage.setLength(content.length);
					ctx.writeAndFlush(tcpTextMessage);
					return;
				}*/

				proxyClient.write(tcpTextMessage);

			} else {

				if (log.isDebugEnabled()) {
					ByteBuf buf = (ByteBuf) msg;
					log.debug("{} {}: {}", proxyConfig.logMsg(), ProxyConfig.LEFT_IN, NettyByteBufUtils.toString(buf));
				}
				/*if (proxyConfig.getServerPort() == 6666) {
					byte[] content = "tcp msg from 6666".getBytes();
					ByteBuf byteBuf = Unpooled.copiedBuffer(content);
					ctx.writeAndFlush(byteBuf);
					return;
				}*/
				proxyClient.write(msg);
			}

		} catch (Exception e) {
			long msgId = (tcpTextMessage != null) ? tcpTextMessage.getId() : Objects.hashCode(msg);
			log.error("{} {} {} exception", proxyConfig.logMsg(), ProxyConfig.LEFT_IN, msgId, e);
			throw e;
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				processReadIdle(ctx);
			} else if (event.state() == IdleState.WRITER_IDLE) {
				processWriteIdle(ctx);
			} else if (event.state() == IdleState.ALL_IDLE) {
				processAllIdle(ctx);
			}
		}
	}

	private void processReadIdle(ChannelHandlerContext ctx) {

	}

	private void processWriteIdle(ChannelHandlerContext ctx) {

	}

	private void processAllIdle(ChannelHandlerContext ctx) {
		String channelId = ctx.channel().id().asLongText();
		this.channelManager.removeClient(channelId);
		ctx.close();
		log.debug("[Netty]connection(id=" + channelId + ") reached max idle time, connection closed.");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error("exception:", cause);
		String channelId = ctx.channel().id().asLongText();
		this.channelManager.removeClient(channelId);
		ctx.close();
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
