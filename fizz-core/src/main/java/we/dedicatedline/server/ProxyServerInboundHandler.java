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

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import we.dedicatedline.ProxyConfig;
import we.dedicatedline.client.ProxyClient;

/**
 * 
 * @author Francis Dong
 *
 */
public class ProxyServerInboundHandler extends ChannelInboundHandlerAdapter {

	private static final Logger log = LoggerFactory.getLogger(ProxyServerInboundHandler.class);

	
	private ChannelManager channelManager;
	private ProxyConfig proxyConfig;

	public ProxyServerInboundHandler(ChannelManager channelManager, ProxyConfig proxyConfig) {
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

	/**
	 * 客户端发消息会触发
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		log.info("proxy channel read......");
		String channelId = ctx.channel().id().asLongText();
		try {
			ProxyClient proxyClient = this.channelManager.getChannelMap().get(channelId);
			if (proxyClient == null) {
				proxyClient = new ProxyClient(this.proxyConfig.getTargetHost(), this.proxyConfig.getTargetPort(), ctx);
				proxyClient.connect();
				this.channelManager.getChannelMap().put(channelId, proxyClient);
			}
			proxyClient.write(msg);
			// 处理业务
		} catch (Exception e) {
			log.warn("", e);
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
