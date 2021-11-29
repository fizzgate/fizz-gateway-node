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
import we.dedicatedline.proxy.codec.FizzSocketMessage;
import we.dedicatedline.proxy.codec.FizzTcpMessage;

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
		log.info("tcp server " + proxyConfig.getServerPort() + " channel read......");

		String channelId = ctx.channel().id().asLongText();
		ProxyClient proxyClient = this.channelManager.getChannelMap().get(channelId);
		if (proxyClient == null) {
			proxyClient = new ProxyClient(null, this.proxyConfig.getProtocol(),
					this.proxyConfig.getTargetHost(), this.proxyConfig.getTargetPort(), ctx, proxyConfig);
			proxyClient.connect();
			this.channelManager.getChannelMap().put(channelId, proxyClient);
		}

		try {
			if (proxyConfig.getRole().equals(ProxyConfig.SERVER)) {
				FizzTcpMessage fizzTcpMessage = (FizzTcpMessage) msg;
				if (log.isDebugEnabled()) {
					log.debug("tcp server {} receive: {}", proxyConfig.getServerPort(), fizzTcpMessage);
				}
				String dedicatedLine = fizzTcpMessage.getDedicatedLineStr();
				long timestamp = fizzTcpMessage.getTimestamp();
				String sign = fizzTcpMessage.getSignStr();
				String sign0 = DedicatedLineUtils.sign(dedicatedLine, timestamp, "ade052c1ec3e44a3bbfbaac988a6e7d4");
				if (sign0.substring(0, FizzSocketMessage.SIGN_LENGTH).equals(sign)) {
				} else {
					byte[] bytes = "tcp msg sign invalid".getBytes();
					fizzTcpMessage.setContent(bytes);
					fizzTcpMessage.setLength(bytes.length);
					ctx.writeAndFlush(fizzTcpMessage);
					return;
				}
				byte[] content = fizzTcpMessage.getContent();
				FizzSocketMessage.inv(content);
				if (log.isDebugEnabled()) {
					log.debug("tcp server {} receive msg content: {}", proxyConfig.getServerPort(), new String(content));
				}
				ByteBuf buf = Unpooled.copiedBuffer(content);
				proxyClient.write(buf);

			} else {
				proxyClient.write(msg);
			}

		} catch (Exception e) {
			log.error("tcp server " + proxyConfig.getServerPort() + " channel read exception", e);
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
		ctx.close();
		log.debug("[Netty]connection(id=" + channelId + ") reached max idle time, connection closed.");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error("exception:", cause);
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

		ProxyClient proxyClient = this.channelManager.getChannelMap().get(channelId);
		if (proxyClient != null) {
			proxyClient.disconnect();
		}
		this.channelManager.getChannelMap().remove(channelId);
	}

}
