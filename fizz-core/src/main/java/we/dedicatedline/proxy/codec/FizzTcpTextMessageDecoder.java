package we.dedicatedline.proxy.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.yetus.audience.InterfaceStability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.dedicatedline.proxy.ProxyConfig;

import java.util.Objects;

/**
 * @author hongqiaowei
 */

@InterfaceStability.Unstable
public class FizzTcpTextMessageDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger log = LoggerFactory.getLogger(FizzTcpTextMessageDecoder.class);

    private ProxyConfig proxyConfig;

    private String direction;

    public FizzTcpTextMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, boolean failFast,
                                     ProxyConfig proxyConfig, String direction) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast);
        this.proxyConfig = proxyConfig;
        this.direction = direction;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {

        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if (frame == null) {
                return null;
            }

            long id = frame.readLong();
            int orderNumber = frame.readInt();

            byte type = frame.readByte();

            byte[] dedicatedLine = new byte[32];
            frame.readBytes(dedicatedLine);

            long timestamp = frame.readLong();

            byte[] sign = new byte[32];
            frame.readBytes(sign);

            int length = frame.readInt();
            if (frame.readableBytes() != length) {
                // TODO: msg id
                String message = proxyConfig.logMsg() + ' ' + direction + ' ' + Objects.hashCode(in) + ": frame length not equal readable length";
                log.error(message);
                throw new DecoderException(message);
            }
            byte[] content = new byte[frame.readableBytes()];
            frame.readBytes(content);
            String s = null;
            if (log.isDebugEnabled()) {
                s = new String(content);
            }
            FizzSocketTextMessage.inv(content);

            FizzTcpTextMessage msg = new FizzTcpTextMessage(id, orderNumber, (int) type, dedicatedLine, timestamp, sign, length, content);
            if (log.isDebugEnabled()) {
                log.debug("{} " + direction + " and decode result: {}, original content: [[{}]]", proxyConfig.logMsg(), msg, s);
            }
            return msg;

        } finally {
            if (frame != null) {
                frame.release(); // ?
            }
        }
    }
}
