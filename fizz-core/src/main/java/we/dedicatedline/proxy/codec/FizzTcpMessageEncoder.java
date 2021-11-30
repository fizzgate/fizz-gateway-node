package we.dedicatedline.proxy.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.yetus.audience.InterfaceStability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hongqiaowei
 */

@InterfaceStability.Unstable
public class FizzTcpMessageEncoder extends MessageToByteEncoder<FizzTcpMessage> {

    private static final Logger log = LoggerFactory.getLogger(FizzTcpMessageEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, FizzTcpMessage msg, ByteBuf out) throws Exception {
        if (msg == null) {
            throw new EncoderException("fizz tcp message is null");
        }
        if (log.isDebugEnabled()) {
            log.debug("encode fizz tcp message: {}", msg);
        }
        byte[] content = msg.getContent();
        FizzSocketMessage.inv(content);
        out.writeByte(  msg.getType());
        out.writeBytes( msg.getDedicatedLine());
        out.writeLong(  msg.getTimestamp());
        out.writeBytes( msg.getSign());
        out.writeInt(   msg.getLength());
        out.writeBytes( content);
    }
}
