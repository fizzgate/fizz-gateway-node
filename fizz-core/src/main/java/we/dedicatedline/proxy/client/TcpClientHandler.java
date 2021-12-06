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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.dedicatedline.DedicatedLineUtils;
import we.dedicatedline.proxy.ProxyConfig;
import we.dedicatedline.proxy.codec.FizzTcpTextMessage;
import we.util.NettyByteBufUtils;

import java.util.Objects;

/**
 * 
 * @author Francis Dong
 *
 */
public class TcpClientHandler extends ChannelInboundHandlerAdapter {

	private static final Logger log = LoggerFactory.getLogger(TcpClientHandler.class);

	private ChannelHandlerContext proxyServerChannelCtx;
	private ProxyClient proxyClient;

	private ProxyConfig proxyConfig;

	public TcpClientHandler(ProxyConfig proxyConfig, ChannelHandlerContext proxyServerChannelCtx, ProxyClient proxyClient) {
		this.proxyConfig = proxyConfig;
		this.proxyServerChannelCtx = proxyServerChannelCtx;
		this.proxyClient = proxyClient;
	}

	/**
	 * 客户端连接会触发
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.info("client channel active......");
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		FizzTcpTextMessage message = null;
		try {
			if (!proxyConfig.isRightIn()) {
				if (log.isDebugEnabled()) {
					log.debug("{} {} {}: {}", proxyConfig.logMsg(), ProxyConfig.RIGHT_IN, Objects.hashCode(msg), NettyByteBufUtils.toString((ByteBuf) msg));
				}

				if (proxyConfig.isLeftOut()) {
					byte[] bytes = NettyByteBufUtils.toBytes((ByteBuf) msg);
					FizzTcpTextMessage fizzTcpMessage = new FizzTcpTextMessage();
					fizzTcpMessage.setDedicatedLine("41d7a1573d054bbca7cbcf4008d7b925");
					fizzTcpMessage.setTimestamp(System.currentTimeMillis());
					String sign = DedicatedLineUtils.sign(fizzTcpMessage.getDedicatedLineStr(), fizzTcpMessage.getTimestamp(), "ade052c1ec3e44a3bbfbaac988a6e7d4");
					fizzTcpMessage.setSign(sign.substring(0, FizzTcpTextMessage.SIGN_LENGTH));
					fizzTcpMessage.setLength(bytes.length);
					fizzTcpMessage.setContent(bytes);
					this.proxyServerChannelCtx.writeAndFlush(fizzTcpMessage);
				} else {
					if (log.isDebugEnabled()) {
						log.debug("{} {} {}: {}", proxyConfig.logMsg(), ProxyConfig.LEFT_OUT, Objects.hashCode(msg), NettyByteBufUtils.toString((ByteBuf) msg));
					}
					this.proxyServerChannelCtx.writeAndFlush(msg);
				}

			} else { // right in
				message = (FizzTcpTextMessage) msg;
				if (proxyConfig.isLeftOut()) {
					this.proxyServerChannelCtx.writeAndFlush(message);
				} else {
					byte[] content = message.getContent();
					ByteBuf buf = NettyByteBufUtils.toByteBuf(content);
					this.proxyServerChannelCtx.writeAndFlush(buf);
					if (log.isDebugEnabled()) {
						log.debug("{} {}: {}", this.proxyConfig.logMsg(), ProxyConfig.LEFT_OUT, new String(content));
					}
				}
			}

		} catch (Exception e) {
			long msgId = (message != null) ? message.getId() : Objects.hashCode(msg);
			log.error("{} {} {} exception", this.proxyConfig.logMsg(), ProxyConfig.RIGHT_IN, msgId, e);
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
		proxyClient.remove();
		proxyClient.disconnect();
		log.debug("[Netty]connection(id=" + channelId + ") reached max idle time, connection closed.");
	}

	/**
	 * 发生异常触发
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
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
		this.proxyServerChannelCtx.close();
		super.channelUnregistered(ctx);
		log.info("client channelUnregistered, channelId={}", ctx.channel().id().asLongText());
	}
}
