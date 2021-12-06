package we.dedicatedline.proxy.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.dedicatedline.DedicatedLineUtils;
import we.dedicatedline.proxy.ProxyConfig;
import we.util.NettyByteBufUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hongqiaowei
 */

public abstract class FizzUdpTextMessageCodec {

    private static final Logger log = LoggerFactory.getLogger(FizzUdpTextMessageCodec.class);

    private FizzUdpTextMessageCodec() {
    }

    public static List<DatagramPacket> disassemble(InetSocketAddress recipient, byte[] contentBytes) {
        List<DatagramPacket> result = new ArrayList<>();
        int contentLen = contentBytes.length;
        long id = FizzSocketTextMessage.ID_GENERATOR.next();
        String dedicatedLine = "41d7a1573d054bbca7cbcf4008d7b925";
        long timestamp = System.currentTimeMillis();
        String sign = DedicatedLineUtils.sign(dedicatedLine, timestamp, "ade052c1ec3e44a3bbfbaac988a6e7d4").substring(0, FizzSocketTextMessage.SIGN_LENGTH);
        for (int start = 0, orderNum = 0; start < contentLen; orderNum++) {
            int end = start + FizzUdpTextMessage.CONTENT_MAX_LENGTH;
            ByteBuf buf = null;
            DatagramPacket msg = null;
            if (end < contentLen) {
                buf = createDatagramPacketByteBuf(FizzUdpTextMessage.MAX_LENGTH, id, orderNum, dedicatedLine, timestamp, sign, contentBytes);
            } else {
                int remainContentLen = contentLen - start;
                buf = createDatagramPacketByteBuf(FizzUdpTextMessage.METADATA_LENGTH + remainContentLen, id, orderNum, dedicatedLine, timestamp, sign, contentBytes);
            }
            msg = new DatagramPacket(buf, recipient);
            result.add(msg);
            start = end;
        }
        return result;
    }

    public static ByteBuf createDatagramPacketByteBuf(int byteBufCap, long id, int orderNum, String dedicatedLine, long timestamp, String sign, byte[] contentBytes) {
        ByteBuf buf = NettyByteBufUtils.alloc(byteBufCap);
        buf.writeLong(  id);
        buf.writeInt(   orderNum);
        buf.writeByte(  0);
        buf.writeBytes( dedicatedLine.getBytes());
        buf.writeLong(  timestamp);
        buf.writeBytes( sign.getBytes());
        buf.writeBytes( contentBytes);
        return buf;
    }

    public static DatagramPacket encode(FizzUdpTextMessage msg, InetSocketAddress recipient, ProxyConfig proxyConfig, String direction) {
        String s = null;
        if (log.isDebugEnabled()) {
            s = msg.toString();
        }
        byte[] content = msg.getContent();
        ByteBuf buf = NettyByteBufUtils.alloc(FizzSocketTextMessage.METADATA_LENGTH + content.length);
        FizzUdpTextMessage.inv(content);
        buf.writeLong(  msg.getId());
        buf.writeInt(   msg.getOrderNumber());
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
        FizzUdpTextMessage message = new FizzUdpTextMessage();

        long id = content.readLong();
        message.setId(id);

        int orderNumber = content.readInt();
        message.setOrderNumber(orderNumber);

        byte type = content.readByte();
        message.setType(type);

        byte[] dlBytes = new byte[FizzSocketTextMessage.DEDICATED_LINE_LENGTH];
        content.readBytes(dlBytes);
        message.setDedicatedLine(dlBytes);

        long timestamp = content.readLong();
        message.setTimestamp(timestamp);

        byte[] signBytes = new byte[FizzSocketTextMessage.SIGN_LENGTH];
        content.readBytes(signBytes);
        message.setSign(signBytes);

        byte[] contentBytes = new byte[content.readableBytes()];
        content.readBytes(contentBytes);

        String s = null;
        if (log.isDebugEnabled()) {
            s = new String(contentBytes);
        }

        FizzSocketTextMessage.inv(contentBytes);
        message.setContent(contentBytes);

        if (log.isDebugEnabled()) {
            log.debug("{} {} decode result: {}, original content: [[{}]]", proxyConfig.logMsg(), direction, message, s);
        }

        return message;
    }
}
