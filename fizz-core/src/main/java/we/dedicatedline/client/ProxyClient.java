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
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import we.dedicatedline.server.ChannelManager;
import we.dedicatedline.server.ProxyServer;

/**
 * 
 * @author Francis Dong
 *
 */
public class ProxyClient {

	private static final Logger log = LoggerFactory.getLogger(ProxyClient.class);

	private EventLoopGroup group;
	private ChannelFuture channelFuture;
	private Channel channel;
	private Bootstrap bootstrap;
	private ChannelHandlerContext proxyServerChannelCtx;
	private String protocol;
	private String host;
	private Integer port;

	public ProxyClient(String protocol, String host, Integer port, ChannelHandlerContext proxyServerChannelCtx) {
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.proxyServerChannelCtx = proxyServerChannelCtx;
		group = new NioEventLoopGroup();

		bootstrap = new Bootstrap();
		switch (protocol) {
		case ProxyServer.PROTOCOL_TCP:
			bootstrap.group(group).channel(NioSocketChannel.class).remoteAddress(host, port)
					.option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) {
							ChannelPipeline pipeline = ch.pipeline();
							pipeline.addLast(new ProxyClientInboundHandler(proxyServerChannelCtx));
						}
					});
			break;
		case ProxyServer.PROTOCOL_UDP:
			bootstrap.group(group).channel(NioDatagramChannel.class)
					.handler(new ChannelInitializer<NioDatagramChannel>() {
						@Override
						protected void initChannel(NioDatagramChannel ch) {
							ChannelPipeline pipeline = ch.pipeline();
							pipeline.addLast(new ProxyClientInboundHandler(proxyServerChannelCtx));
						}
					});
			break;
		}
	}

	public void connect() {
		try {
			switch (protocol) {
			case ProxyServer.PROTOCOL_TCP:
				channelFuture = bootstrap.connect().sync();
				channel = (Channel) channelFuture.channel();
				break;
			case ProxyServer.PROTOCOL_UDP:
				channelFuture = bootstrap.bind(0).sync();
				channel = (Channel) channelFuture.channel();
				break;
			}
		} catch (InterruptedException e) {
			log.warn("connect failed, host: {}, port: {}", host, port, e);
		}

//		channelFuture = bootstrap.connect();
//		// 客户端断线重连逻辑
//		channelFuture.addListener((ChannelFutureListener) future1 -> {
//			if (future1.isSuccess()) {
//				log.info("connected");
//			} else {
//				log.info("connect failed, retry in 2 seconds");
//				future1.channel().eventLoop().schedule(() -> connect(), 2, TimeUnit.SECONDS);
//			}
//		});
//		socketChannel = (SocketChannel) channelFuture.channel();
	}

	public void disconnect() {
		if (channelFuture != null && channelFuture.channel().isRegistered()) {
			try {
				channelFuture.channel().close().sync();
			} catch (InterruptedException e) {
				log.warn("disconnect failed, host: {}, port: {}", host, port, e);
			}
		}
	}

	public void write(Object msg) {
		if (this.channel != null) {
			switch (protocol) {
			case ProxyServer.PROTOCOL_TCP:
				this.channel.writeAndFlush(msg);
				break;
			case ProxyServer.PROTOCOL_UDP:
				InetSocketAddress address = new InetSocketAddress(host, port);
				if (msg instanceof DatagramPacket) {
					DatagramPacket dp = (DatagramPacket) msg;
					this.channel.writeAndFlush(new DatagramPacket(dp.content(), address));
				} else {
					ByteBuf byteBuf = Unpooled.copiedBuffer(msg.toString().getBytes(StandardCharsets.UTF_8));
					this.channel.writeAndFlush(new DatagramPacket(byteBuf, address));
				}
				break;
			}
		}
	}

}
