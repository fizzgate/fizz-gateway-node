package we.dedicatedline.proxy.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.apache.yetus.audience.InterfaceStability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.dedicatedline.DedicatedLineUtils;
import we.dedicatedline.proxy.ProxyConfig;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hongqiaowei
 */

@InterfaceStability.Unstable
public class FizzUdpTextMessage extends FizzSocketTextMessage {

    private static final Logger log = LoggerFactory.getLogger(FizzUdpTextMessage.class);

    public static int MAX_LENGTH         = 4096;

    public static int CONTENT_MAX_LENGTH = MAX_LENGTH - METADATA_LENGTH;

    public FizzUdpTextMessage() {
    }

    public FizzUdpTextMessage(int type, byte[] dedicatedLine, long timestamp, byte[] sign, byte[] content) {
        super(0L, 0, type, dedicatedLine, timestamp, sign, content);
    }

    public FizzUdpTextMessage(int type, String dedicatedLine, long timestamp, String sign, String content) {
        this(type, dedicatedLine.getBytes(), timestamp, sign.getBytes(), content.getBytes());
    }

    // TODO：下面几个方法搬走

    public static List<DatagramPacket> disassemble(InetSocketAddress recipient, byte[] contentBytes) {
        List<DatagramPacket> result = new ArrayList<>();
        int cl = contentBytes.length;
        long msgId = FizzSocketTextMessage.ID_GENERATOR.next();
        String dedicatedLine = "41d7a1573d054bbca7cbcf4008d7b925";
        long timestamp = System.currentTimeMillis();
        String sign = DedicatedLineUtils.sign(dedicatedLine, timestamp, "ade052c1ec3e44a3bbfbaac988a6e7d4").substring(0, FizzSocketTextMessage.SIGN_LENGTH);
        for (int start = 0, on = 0; start < cl; on++) {
            int end = start + FizzUdpTextMessage.CONTENT_MAX_LENGTH;
            ByteBuf buf = null;
            DatagramPacket msg = null;
            if (end < cl) {
                buf = createDatagramPacketByteBuf(FizzUdpTextMessage.MAX_LENGTH, msgId ,on, dedicatedLine, timestamp, sign, contentBytes/*,  start, FizzUdpTextMessage.CONTENT_MAX_LENGTH*/);
            } else {
                int contentLength = cl - start;
                buf = createDatagramPacketByteBuf(FizzUdpTextMessage.METADATA_LENGTH + contentLength, msgId ,on, dedicatedLine, timestamp, sign, contentBytes/*, start, contentLength*/);
            }
            msg = new DatagramPacket(buf, recipient);
            result.add(msg);
            start = end;
        }
        return result;
    }

    public static ByteBuf createDatagramPacketByteBuf( int byteBufCap, long id, int on, String dedicatedLine, long timestamp, String sign, byte[] contentBytes/*, int start, int extractedLength*/) {
//        byte[] bytes = new byte[byteBufCap];
//
//        bytes[0] = 1;
//
//        byte[] dlBytes = dedicatedLine.getBytes();
//        System.arraycopy(dlBytes, 0, bytes, 1, FizzSocketTextMessage.DEDICATED_LINE_LENGTH);
//
//        long timestamp = System.currentTimeMillis();
//        byte[] timestampBytes = ByteBuffer.allocate(Long.BYTES).putLong(0, timestamp).array();
//        System.arraycopy(timestampBytes, 0, bytes, 33, FizzSocketTextMessage.TIMESTAMP_LENGTH);
//
//        String sign = DedicatedLineUtils.sign(dedicatedLine, timestamp, "ade052c1ec3e44a3bbfbaac988a6e7d4");
//        byte[] signBytes = sign.substring(0, FizzSocketTextMessage.SIGN_LENGTH).getBytes();
//        System.arraycopy(signBytes, 0, bytes, 41, FizzSocketTextMessage.SIGN_LENGTH);
//
//        System.arraycopy(contentBytes, start, bytes, 73, extractedLength);
//        FizzSocketTextMessage.inv(73, bytes);
//
//        return Unpooled.copiedBuffer(bytes);


        ByteBuf buf = Unpooled.buffer(byteBufCap);
        buf.writeLong(id);
        buf.writeInt(on);
        buf.writeByte(0);
        buf.writeBytes(dedicatedLine.getBytes());

                buf.writeLong(timestamp);
                buf.writeBytes(sign.getBytes());
        buf.writeBytes(contentBytes);
return buf;
    }

    public static DatagramPacket encode(FizzUdpTextMessage msg, InetSocketAddress recipient, ProxyConfig proxyConfig, String direction) {
        ByteBuf buf = Unpooled.buffer();

        String s = null;
        if (log.isDebugEnabled()) {
            s = msg.toString();
        }


        byte[] content = msg.getContent();
        FizzUdpTextMessage.inv(content);
        buf.writeLong(  msg.getId());
        buf.writeInt(  msg.getOrderNumber());
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

    public static FizzUdpTextMessage decode(DatagramPacket msg, ProxyConfig proxyConfig, String direction) {
        ByteBuf content = msg.content();
        FizzUdpTextMessage fizzUdpMessage = new FizzUdpTextMessage();

        long id = content.readLong();
        fizzUdpMessage.setId(id);

        int orderNumber = content.readInt();
        fizzUdpMessage.setOrderNumber(orderNumber);

        byte type = content.readByte();
        fizzUdpMessage.setType(type);

        byte[] dlBytes = new byte[FizzSocketTextMessage.DEDICATED_LINE_LENGTH];
        content.readBytes(dlBytes);
        fizzUdpMessage.setDedicatedLine(dlBytes);

        long timestamp = content.readLong();
        fizzUdpMessage.setTimestamp(timestamp);

        byte[] signBytes = new byte[FizzSocketTextMessage.SIGN_LENGTH];
        content.readBytes(signBytes);
        fizzUdpMessage.setSign(signBytes);

        byte[] contentBytes = new byte[content.readableBytes()];
        content.readBytes(contentBytes);

        String s = null;
        if (log.isDebugEnabled()) {
            s = new String(contentBytes);
        }

        FizzSocketTextMessage.inv(contentBytes);
        fizzUdpMessage.setContent(contentBytes);

        if (log.isDebugEnabled()) {
            log.debug("{} {} decode result: {}, original content: [[{}]]", proxyConfig.logMsg(), direction, fizzUdpMessage, s);
        }

        return fizzUdpMessage;
    }
}
