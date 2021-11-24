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

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import we.dedicatedline.ProxyConfig;

/**
 * 
 * @author Francis Dong
 *
 */
public class ProxyServer {

	private static final Logger log = LoggerFactory.getLogger(ProxyServer.class);

	public final static int READER_IDLE_TIME_SECONDS = 30;
	public final static int WRITER_IDLE_TIME_SECONDS = 30;
	public final static int ALL_IDLE_TIME_SECONDS = 30;

	public static final String PROTOCOL_TCP = "TCP";
	public static final String PROTOCOL_UDP = "UDP";

	private EventLoopGroup boss;
	private EventLoopGroup work;
	private ServerBootstrap serverBootstrap;
	private Bootstrap bootstrap;
	private ChannelManager channelManager;
	private ChannelFuture channelFuture;
	private ProxyConfig proxyConfig;

	public ProxyServer(ProxyConfig proxyConfig) throws InterruptedException {
		this.proxyConfig = proxyConfig;
		channelManager = new ChannelManager();

		switch (proxyConfig.getProtocol()) {
		case PROTOCOL_TCP:
			boss = new NioEventLoopGroup(1);
			work = new NioEventLoopGroup();
			serverBootstrap = new ServerBootstrap();
			serverBootstrap.group(boss, work).channel(NioServerSocketChannel.class)
					.localAddress(new InetSocketAddress(proxyConfig.getServerPort()))
					.option(ChannelOption.SO_BACKLOG, 4096).option(ChannelOption.SO_REUSEADDR, true)
					.childOption(ChannelOption.SO_KEEPALIVE, true).childOption(ChannelOption.TCP_NODELAY, true)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) {
							ChannelPipeline pipeline = ch.pipeline();
							ch.pipeline().addLast(new IdleStateHandler(READER_IDLE_TIME_SECONDS,
									WRITER_IDLE_TIME_SECONDS, ALL_IDLE_TIME_SECONDS));
							pipeline.addLast(new TcpServerHandler(channelManager, proxyConfig));
						}
					});
			break;
		case PROTOCOL_UDP:
			boss = new NioEventLoopGroup();
			bootstrap = new Bootstrap();
			bootstrap.group(boss).channel(NioDatagramChannel.class)
					.localAddress(new InetSocketAddress(proxyConfig.getServerPort()))
					.option(ChannelOption.SO_BACKLOG, 4096).option(ChannelOption.SO_BROADCAST, true)
					.handler(new ChannelInitializer<NioDatagramChannel>() {
						@Override
						protected void initChannel(NioDatagramChannel ch) {
							ChannelPipeline pipeline = ch.pipeline();
							pipeline.addLast(new TcpServerHandler(channelManager, proxyConfig));
						}
					});
			break;
		default:
			log.warn("{} protocol is not supported", proxyConfig.getProtocol());
			break;
		}
	}

	public void start() throws InterruptedException {
		switch (proxyConfig.getProtocol()) {
		case PROTOCOL_TCP:
			channelFuture = serverBootstrap.bind().sync();
			break;
		case PROTOCOL_UDP:
			channelFuture = bootstrap.bind().sync();
			break;
		}
		if (channelFuture == null) {
			return;
		}

		if (channelFuture.isSuccess()) {
			log.info("proxy server started, port: {}", proxyConfig.getServerPort());
		} else {
			log.info("failed to start proxy server, port: {}", proxyConfig.getServerPort());
		}
	}

	public void stop() throws InterruptedException {
		// disconnect proxy client's connections

		// shutdown server
		if (boss != null) {
			boss.shutdownGracefully().sync();
		}
		if (work != null) {
			work.shutdownGracefully().sync();
		}
		log.info("proxy server stopped, port: {}", proxyConfig.getServerPort());
	}

	public ProxyConfig getProxyConfig() {
		return proxyConfig;
	}

}
