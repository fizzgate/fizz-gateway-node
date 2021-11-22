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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import we.dedicatedline.ProxyConfig;

/**
 * 
 * @author Francis Dong
 *
 */
public class ProxyServer {

	private static final Logger log = LoggerFactory.getLogger(ProxyServer.class);

	private EventLoopGroup boss;
	private EventLoopGroup work;
	private ServerBootstrap bootstrap;
	private ChannelManager channelManager;
	private ChannelFuture channelFuture;
	private ProxyConfig proxyConfig;

	public ProxyServer(ProxyConfig proxyConfig) throws InterruptedException {
		this.proxyConfig = proxyConfig;
		channelManager = new ChannelManager();
		boss = new NioEventLoopGroup(1);
		work = new NioEventLoopGroup();

		bootstrap = new ServerBootstrap();
		bootstrap.group(boss, work).channel(NioServerSocketChannel.class)
				.localAddress(new InetSocketAddress(proxyConfig.getServerPort())).option(ChannelOption.SO_BACKLOG, 4096)
				.option(ChannelOption.SO_REUSEADDR, true).childOption(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.TCP_NODELAY, true)
				.childHandler(new ProxyServerChannelInitializer(channelManager, proxyConfig));
	}

	public void start() throws InterruptedException {
		channelFuture = bootstrap.bind().sync();
		if (channelFuture.isSuccess()) {
			log.info("proxy server started, port: {}", proxyConfig.getServerPort());
		} else {
			log.info("failed to start proxy server, port: {}", proxyConfig.getServerPort());
		}
	}

	public void stop() throws InterruptedException {
		// disconnect proxy client's connections

		// shutdown server
		boss.shutdownGracefully().sync();
		work.shutdownGracefully().sync();
		log.info("proxy server stopped, port: {}", proxyConfig.getServerPort());
	}

	public ProxyConfig getProxyConfig() {
		return proxyConfig;
	}


}
