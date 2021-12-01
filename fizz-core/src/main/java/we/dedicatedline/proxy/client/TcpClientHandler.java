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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.dedicatedline.DedicatedLineUtils;
import we.dedicatedline.proxy.ProxyConfig;
import we.dedicatedline.proxy.codec.FizzTcpMessage;

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
//		log.info("tcp client to {}:{} channel read ...", this.proxyConfig.getTargetHost(), this.proxyConfig.getTargetPort());
		// String channelId = ctx.channel().id().asLongText();
		try {
			// if (proxyConfig.getRole().equals(ProxyConfig.SERVER)) {
			if (!proxyConfig.isRightIn()) {
//				ByteBuf buf = (ByteBuf) msg;
//				byte[] bytes = new byte[buf.readableBytes()];
//				buf.readBytes(bytes);
//				if (log.isDebugEnabled()) {
//					log.debug("{} right in: {}", this.proxyConfig.logMsg(),  new String(bytes));
//				}
				String s = null;
				if (log.isDebugEnabled()) {
					ByteBuf buf = (ByteBuf) msg;
					ByteBuf copy = buf.copy();
					byte[] bytes = new byte[copy.readableBytes()]; // TODO: util
					copy.readBytes(bytes);
					s = new String(bytes);
					log.debug("{} right in: {}", proxyConfig.logMsg(), s);
				}



				if (proxyConfig.isLeftOut()) {
					ByteBuf buf = (ByteBuf) msg;
					byte[] bytes = new byte[buf.readableBytes()];
					buf.readBytes(bytes);
					FizzTcpMessage fizzTcpMessage = new FizzTcpMessage();
					fizzTcpMessage.setType(1);
					fizzTcpMessage.setDedicatedLine("41d7a1573d054bbca7cbcf4008d7b925"); // TODO
					fizzTcpMessage.setTimestamp(System.currentTimeMillis());
					String sign = DedicatedLineUtils.sign(fizzTcpMessage.getDedicatedLineStr(), fizzTcpMessage.getTimestamp(), "ade052c1ec3e44a3bbfbaac988a6e7d4");
					fizzTcpMessage.setSign(sign.substring(0, FizzTcpMessage.SIGN_LENGTH));
					fizzTcpMessage.setLength(bytes.length);
					fizzTcpMessage.setContent(bytes);
					this.proxyServerChannelCtx.writeAndFlush(fizzTcpMessage);
//					if (log.isDebugEnabled()) {
//						log.debug("tcp client to {}:{} send: {}", this.proxyConfig.getTargetHost(), this.proxyConfig.getTargetPort(), fizzTcpMessage);
//					}
				} else {

					this.proxyServerChannelCtx.writeAndFlush(msg);
					if (log.isDebugEnabled()) {
						log.debug("{} left out: {}", proxyConfig.logMsg(), s);
					}
				}

			} else { // right in
				FizzTcpMessage fizzTcpMessage = (FizzTcpMessage) msg;
//				if (log.isDebugEnabled()) {
//					log.debug("tcp client to {}:{} receive: {}", this.proxyConfig.getTargetHost(), this.proxyConfig.getTargetPort(), fizzTcpMessage);
//				}


				if (proxyConfig.isLeftOut()) {
					this.proxyServerChannelCtx.writeAndFlush(fizzTcpMessage);
//					if (log.isDebugEnabled()) {
//						log.debug("tcp client to {}:{} response client: {}", this.proxyConfig.getTargetHost(), this.proxyConfig.getTargetPort(), fizzTcpMessage);
//					}
				} else {
					byte[] content = fizzTcpMessage.getContent();
					ByteBuf buf = Unpooled.copiedBuffer(content);
					this.proxyServerChannelCtx.writeAndFlush(buf);
					if (log.isDebugEnabled()) {
						log.debug("{} left out: {}", this.proxyConfig.logMsg(), new String(content));
					}
				}
			}

		} catch (Exception e) {
			log.error("{} right in exception", this.proxyConfig.logMsg(), e);
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
