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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import we.dedicatedline.server.ChannelManager;

/**
 * 
 * @author Francis Dong
 *
 */
public class ProxyClientChannelInitializer extends ChannelInitializer<SocketChannel> {

	public final static int READER_IDLE_TIME_SECONDS = 30;
	public final static int WRITER_IDLE_TIME_SECONDS = 30;
	public final static int ALL_IDLE_TIME_SECONDS = 30;

	private ChannelHandlerContext proxyServerChannelCtx;

	public ProxyClientChannelInitializer(ChannelHandlerContext proxyServerChannelCtx) {
		this.proxyServerChannelCtx = proxyServerChannelCtx;
	}

	@Override
	protected void initChannel(SocketChannel socketChannel) throws Exception {
		socketChannel.pipeline().addLast(
				new IdleStateHandler(READER_IDLE_TIME_SECONDS, WRITER_IDLE_TIME_SECONDS, ALL_IDLE_TIME_SECONDS));

		socketChannel.pipeline().addLast(new ProxyClientInboundHandler(this.proxyServerChannelCtx));
	}
}