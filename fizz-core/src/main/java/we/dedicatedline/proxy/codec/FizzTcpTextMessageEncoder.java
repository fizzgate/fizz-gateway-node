package we.dedicatedline.proxy.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.yetus.audience.InterfaceStability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.dedicatedline.proxy.ProxyConfig;

/**
 * @author hongqiaowei
 */

@InterfaceStability.Unstable
public class FizzTcpTextMessageEncoder extends MessageToByteEncoder<FizzTcpTextMessage> {

    private static final Logger log = LoggerFactory.getLogger(FizzTcpTextMessageEncoder.class);

    private ProxyConfig proxyConfig;

    private String direction;

    public FizzTcpTextMessageEncoder(ProxyConfig proxyConfig, String direction) {
        this.proxyConfig = proxyConfig;
        this.direction = direction;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, FizzTcpTextMessage msg, ByteBuf out) throws Exception {
        if (msg == null) {
            String message = proxyConfig.logMsg() + ' ' + direction + ":fizz tcp message is null";
            log.error(message);
            throw new EncoderException(message);
        }
        String s = null;
        if (log.isDebugEnabled()) {
            s = msg.toString();
        }
        byte[] content = msg.getContent();

        FizzSocketTextMessage.inv(content);
        out.writeLong(  msg.getId());
        out.writeInt(  msg.getOrderNumber());
        out.writeByte(  msg.getType());
        out.writeBytes( msg.getDedicatedLine());
        out.writeLong(  msg.getTimestamp());
        out.writeBytes( msg.getSign());
        out.writeInt(   msg.getLength());
        out.writeBytes( content);
        if (log.isDebugEnabled()) {
            log.debug("{} {} encode: {}, content encrypted: [[{}]]", proxyConfig.logMsg(), direction, s , new String(content));
        }
    }
}
