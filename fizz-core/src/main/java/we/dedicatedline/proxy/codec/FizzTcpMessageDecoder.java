package we.dedicatedline.proxy.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.yetus.audience.InterfaceStability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hongqiaowei
 */

@InterfaceStability.Unstable
public class FizzTcpMessageDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger log = LoggerFactory.getLogger(FizzTcpMessageDecoder.class);

    public FizzTcpMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {

        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if (frame == null) {
                return null;
            }

            byte type = frame.readByte();

            byte[] dedicatedLine = new byte[32];
            frame.readBytes(dedicatedLine);

            long timestamp = frame.readLong();

            byte[] sign = new byte[32];
            frame.readBytes(sign);

            int length = frame.readInt();
            if (frame.readableBytes() != length) {
                throw new DecoderException("frame length not equal readable length");
            }
            byte[] content = new byte[frame.readableBytes()];
            frame.readBytes(content);
            FizzSocketMessage.inv(content);

            FizzTcpMessage msg = new FizzTcpMessage(type, dedicatedLine, timestamp, sign, length, content);
            if (log.isDebugEnabled()) {
                log.debug("decode result: {}", msg.toString());
            }
            return msg;

        } finally {
            if (frame != null) {
                frame.release(); // ?
            }
        }
    }
}
