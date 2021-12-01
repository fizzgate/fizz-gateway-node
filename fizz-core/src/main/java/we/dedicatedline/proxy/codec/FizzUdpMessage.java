package we.dedicatedline.proxy.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.apache.yetus.audience.InterfaceStability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.dedicatedline.DedicatedLineUtils;
import we.dedicatedline.proxy.ProxyConfig;
import we.dedicatedline.proxy.server.UdpServerHandler;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hongqiaowei
 */

@InterfaceStability.Unstable
public class FizzUdpMessage extends FizzSocketMessage {

    private static final Logger log = LoggerFactory.getLogger(FizzUdpMessage.class);

    public static int MAX_LENGTH         = 4096;

    public static int CONTENT_MAX_LENGTH = MAX_LENGTH - METADATA_LENGTH;

    public FizzUdpMessage() {
    }

    public FizzUdpMessage(int type, byte[] dedicatedLine, long timestamp, byte[] sign, byte[] content) {
        super(type, dedicatedLine, timestamp, sign, content);
    }

    public FizzUdpMessage(int type, String dedicatedLine, long timestamp, String sign, String content) {
        super(type, dedicatedLine, timestamp, sign, content);
    }

    // TODO：下面几个方法搬走

    public static List<DatagramPacket> disassemble(InetSocketAddress recipient, byte[] contentBytes) {
        List<DatagramPacket> result = new ArrayList<>();
        int cl = contentBytes.length;
        for (int start = 0; start < cl; ) {
            int end = start + FizzUdpMessage.CONTENT_MAX_LENGTH;
            ByteBuf buf = null;
            DatagramPacket msg = null;
            if (end < cl) {
                buf = createDatagramPacketByteBuf(FizzUdpMessage.MAX_LENGTH, "41d7a1573d054bbca7cbcf4008d7b925", contentBytes, start, FizzUdpMessage.CONTENT_MAX_LENGTH);
            } else {
                int contentLength = cl - start;
                buf = createDatagramPacketByteBuf(FizzUdpMessage.METADATA_LENGTH + contentLength, "41d7a1573d054bbca7cbcf4008d7b925", contentBytes, start, contentLength);
            }
            msg = new DatagramPacket(buf, recipient);
            result.add(msg);
            start = end;
        }
        return result;
    }

    public static ByteBuf createDatagramPacketByteBuf(int byteBufCap, String dedicatedLine, byte[] contentBytes, int start, int extractedLength) {
        byte[] bytes = new byte[byteBufCap];

        bytes[0] = 1;

        byte[] dlBytes = dedicatedLine.getBytes();
        System.arraycopy(dlBytes, 0, bytes, 1, FizzSocketMessage.DEDICATED_LINE_LENGTH);

        long timestamp = System.currentTimeMillis();
        byte[] timestampBytes = ByteBuffer.allocate(Long.BYTES).putLong(0, timestamp).array();
        System.arraycopy(timestampBytes, 0, bytes, 33, FizzSocketMessage.TIMESTAMP_LENGTH);

        String sign = DedicatedLineUtils.sign(dedicatedLine, timestamp, "ade052c1ec3e44a3bbfbaac988a6e7d4");
        byte[] signBytes = sign.substring(0, FizzSocketMessage.SIGN_LENGTH).getBytes();
        System.arraycopy(signBytes, 0, bytes, 41, FizzSocketMessage.SIGN_LENGTH);

        System.arraycopy(contentBytes, start, bytes, 73, extractedLength);
        FizzSocketMessage.inv(73, bytes);

        return Unpooled.copiedBuffer(bytes);
    }

    public static DatagramPacket encode(FizzUdpMessage msg, InetSocketAddress recipient, ProxyConfig proxyConfig, String direction) {
        ByteBuf buf = Unpooled.buffer();

        String s = null;
        if (log.isDebugEnabled()) {
            s = msg.toString();
        }


        byte[] content = msg.getContent();
        FizzUdpMessage.inv(content);
        buf.writeByte(  msg.getType());
        buf.writeBytes( msg.getDedicatedLine());
        buf.writeLong(  msg.getTimestamp());
        buf.writeBytes( msg.getSign());
        buf.writeBytes( content);

        if (log.isDebugEnabled()) {
            log.debug("{} {} encode: {}, content encrypted: [[{}]]", proxyConfig.logMsg(), direction, s, new String(content));
        }

        return new DatagramPacket(buf, recipient);
    }

    public static FizzUdpMessage decode(DatagramPacket msg, ProxyConfig proxyConfig, String direction) {
        ByteBuf content = msg.content();
        FizzUdpMessage fizzUdpMessage = new FizzUdpMessage();

        byte type = content.readByte();
        fizzUdpMessage.setType(type);

        byte[] dlBytes = new byte[FizzSocketMessage.DEDICATED_LINE_LENGTH];
        content.readBytes(dlBytes);
        fizzUdpMessage.setDedicatedLine(dlBytes);

        long timestamp = content.readLong();
        fizzUdpMessage.setTimestamp(timestamp);

        byte[] signBytes = new byte[FizzSocketMessage.SIGN_LENGTH];
        content.readBytes(signBytes);
        fizzUdpMessage.setSign(signBytes);

        byte[] contentBytes = new byte[content.readableBytes()];
        content.readBytes(contentBytes);

        String s = null;
        if (log.isDebugEnabled()) {
            s = new String(contentBytes);
        }

        FizzSocketMessage.inv(contentBytes);
        fizzUdpMessage.setContent(contentBytes);

        if (log.isDebugEnabled()) {
            log.debug("{} {} decode result: {}, original content: [[{}]]", proxyConfig.logMsg(), direction, fizzUdpMessage, s);
        }

        return fizzUdpMessage;
    }
}
