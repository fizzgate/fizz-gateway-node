package we.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.CharsetUtil;

/**
 * @author hongqiaowei
 */

public abstract class NettyByteBufUtils {

    private NettyByteBufUtils() {
    }

    public static ByteBuf alloc(int size) {
        return ByteBufAllocator.DEFAULT.buffer(size, size);
    }

    public static ByteBuf toByteBuf(byte[] bytes) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(bytes.length, bytes.length);
        buf.writeBytes(bytes);
        return buf;
    }

    public static String toString(ByteBuf buf) {
        buf.markReaderIndex();
        String s = buf.toString(CharsetUtil.UTF_8);
        buf.resetReaderIndex();
        return s;
    }

    public static byte[] toBytes(ByteBuf buf) {
        if (buf.hasArray()) {
            return buf.array();
        }
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }
}
