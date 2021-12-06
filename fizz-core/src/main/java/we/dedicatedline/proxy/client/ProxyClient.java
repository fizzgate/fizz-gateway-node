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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.dedicatedline.DedicatedLineUtils;
import we.dedicatedline.proxy.ProxyConfig;
import we.dedicatedline.proxy.codec.*;
import we.dedicatedline.proxy.server.ChannelManager;
import we.dedicatedline.proxy.server.ProxyServer;
import we.util.NettyByteBufUtils;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 
 * @author Francis Dong
 *
 */
public class ProxyClient {

	private static final Logger log = LoggerFactory.getLogger(ProxyClient.class);

	private String key;
	private ChannelManager channelManager;
	private EventLoopGroup group;
	private ChannelFuture channelFuture;
	private Channel channel;
	private Bootstrap bootstrap;
	private ChannelHandlerContext proxyServerChannelCtx;
	private String protocol;
	private String host;
	private Integer port;
	private InetSocketAddress senderAddress;

	private ProxyConfig proxyConfig;

	public ProxyClient(String key, InetSocketAddress senderAddress, String protocol, String host, Integer port, ChannelHandlerContext proxyServerChannelCtx, ProxyConfig proxyConfig, ChannelManager channelManager) {
		this.key = key;
		this.channelManager = channelManager;
		this.senderAddress = senderAddress;
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.proxyServerChannelCtx = proxyServerChannelCtx;
		this.proxyConfig = proxyConfig;
		group = new NioEventLoopGroup();

		bootstrap = new Bootstrap();
		switch (protocol) {
		case ProxyServer.PROTOCOL_TCP:
			bootstrap.group(group).channel(NioSocketChannel.class).remoteAddress(this.host, this.port)
					.option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) {
							ChannelPipeline pipeline = ch.pipeline();

							if (proxyConfig.isRightOut()) {
								pipeline.addLast("FizzTcpTextMessageEncoder", new FizzTcpTextMessageEncoder(proxyConfig, ProxyConfig.RIGHT_OUT));
								log.info("{} right out add FizzTcpTextMessageEncoder", proxyConfig.logMsg());
							}

							if (proxyConfig.isRightIn()) {
								pipeline.addLast("FizzTcpTextMessageDecoder", new FizzTcpTextMessageDecoder(proxyConfig.getTcpMessageMaxLength(), FizzTcpTextMessage.LENGTH_FIELD_OFFSET,
										FizzTcpTextMessage.LENGTH_FIELD_LENGTH, FizzTcpTextMessage.LENGTH_ADJUSTMENT, FizzTcpTextMessage.INITIAL_BYTES_TO_STRIP, true,
										proxyConfig, ProxyConfig.RIGHT_IN));
								log.info("{} right in add FizzTcpTextMessageDecoder", proxyConfig.logMsg());
							}

							pipeline.addLast(new TcpClientHandler(proxyConfig, proxyServerChannelCtx, ProxyClient.this));
						}
					});
			break;
		case ProxyServer.PROTOCOL_UDP:
			bootstrap.group(group).channel(NioDatagramChannel.class)
					.handler(new ChannelInitializer<NioDatagramChannel>() {
						@Override
						protected void initChannel(NioDatagramChannel ch) {
							ChannelPipeline pipeline = ch.pipeline();
							pipeline.addLast(new IdleStateHandler(0, 0, 60 * 30));
							pipeline.addLast(new UdpClientHandler(senderAddress, proxyServerChannelCtx, ProxyClient.this, proxyConfig));
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

	public void remove() {
		channelManager.remove(key, this);
	}

	public void write(Object msg) {
		if (this.channel != null) {
			switch (protocol) {
			case ProxyServer.PROTOCOL_TCP:

				if (proxyConfig.isLeftIn() && proxyConfig.isRightOut()) {
					this.channel.writeAndFlush(msg);
					return;
				}

				if (!proxyConfig.isLeftIn() && proxyConfig.isRightOut()) {
					byte[] bytes = NettyByteBufUtils.toBytes((ByteBuf) msg);
					FizzTcpTextMessage fizzTcpMessage = new FizzTcpTextMessage();
					fizzTcpMessage.setDedicatedLine("41d7a1573d054bbca7cbcf4008d7b925");
					fizzTcpMessage.setTimestamp(System.currentTimeMillis());
					String sign = DedicatedLineUtils.sign(fizzTcpMessage.getDedicatedLineStr(), fizzTcpMessage.getTimestamp(), "ade052c1ec3e44a3bbfbaac988a6e7d4");
					fizzTcpMessage.setSign(sign.substring(0, FizzTcpTextMessage.SIGN_LENGTH));
					fizzTcpMessage.setLength(bytes.length);
					fizzTcpMessage.setContent(bytes);
					this.channel.writeAndFlush(fizzTcpMessage);
					return;
				}

				if (log.isDebugEnabled()) {
					log.debug("{} {}: {}", proxyConfig.logMsg(), ProxyConfig.RIGHT_OUT, NettyByteBufUtils.toString((ByteBuf) msg));
				}
				this.channel.writeAndFlush(msg);

				break;

			case ProxyServer.PROTOCOL_UDP:

				InetSocketAddress address = new InetSocketAddress(host, port);

				if (proxyConfig.isLeftIn() && proxyConfig.isRightOut()) {
					FizzUdpTextMessage fizzUdpMessage = (FizzUdpTextMessage) msg;
					DatagramPacket enc = FizzUdpTextMessageCodec.encode(fizzUdpMessage, address, proxyConfig, ProxyConfig.RIGHT_OUT);
					channel.writeAndFlush(enc);
					return;
				}

				ByteBuf buf = (ByteBuf) msg;
				if (!proxyConfig.isLeftIn() && proxyConfig.isRightOut()) {
					byte[] bytes = NettyByteBufUtils.toBytes(buf);
					List<DatagramPacket> packets = FizzUdpTextMessageCodec.disassemble(address, bytes);
					for (DatagramPacket pk : packets) {
						if (log.isDebugEnabled()) {
							log.debug("{} {}: {}", proxyConfig.logMsg(), ProxyConfig.RIGHT_OUT, NettyByteBufUtils.toString(pk.content()));
						}
						channel.writeAndFlush(pk);
					}
					return;
				}

				DatagramPacket packet = new DatagramPacket(buf, address);
				if (log.isDebugEnabled()) {
					log.debug("{} {}: {}", proxyConfig.logMsg(), ProxyConfig.RIGHT_OUT, NettyByteBufUtils.toString(packet.content()));
				}
				channel.writeAndFlush(packet);
				break;
			}
		}
	}
}
