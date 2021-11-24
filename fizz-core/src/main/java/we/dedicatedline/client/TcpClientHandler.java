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

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import we.dedicatedline.server.ProxyServer;

/**
 * 
 * @author Francis Dong
 *
 */
public class TcpClientHandler extends ChannelInboundHandlerAdapter {

	private static final Logger log = LoggerFactory.getLogger(TcpClientHandler.class);

	private ChannelHandlerContext proxyServerChannelCtx;
	private String protocol;

	public TcpClientHandler(ChannelHandlerContext proxyServerChannelCtx) {
		this.proxyServerChannelCtx = proxyServerChannelCtx;
	}

	/**
	 * 客户端连接会触发
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.info("client channel active......");
	}

	/**
	 * 客户端发消息会触发
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		log.info("client channel read......");
		String channelId = ctx.channel().id().asLongText();
		try {
			this.proxyServerChannelCtx.writeAndFlush(msg);
		} catch (Exception e) {
		} finally {
			// 需要自己手动的释放的消息
//			ReferenceCountUtil.release(msg);
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

	/**
	 * 发生异常触发
	 */
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
