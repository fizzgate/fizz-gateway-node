/*
 *  Copyright (C) 2020 the original author or authors.
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

package we.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.lang.Nullable;
import we.flume.clients.log4j2appender.LogService;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author hongqiaowei
 */

public abstract class NettyDataBufferUtils extends org.springframework.core.io.buffer.DataBufferUtils {

    private static final Logger log = LoggerFactory.getLogger(NettyDataBufferUtils.class);

    private static NettyDataBufferFactory dataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

    public static NettyDataBuffer from(String s) {
        return from(s.getBytes(StandardCharsets.UTF_8));
    }

    public static NettyDataBuffer from(byte[] bytes) {
        return (NettyDataBuffer) dataBufferFactory.wrap(bytes);
    }

    public static NettyDataBuffer from(ByteBuffer byteBuffer) {
        return dataBufferFactory.wrap(byteBuffer);
    }

    public static NettyDataBuffer from(ByteBuf byteBuf) {
        return dataBufferFactory.wrap(byteBuf);
    }

    public static boolean release(@Nullable String reqId, @Nullable DataBuffer dataBuffer) {
        if (dataBuffer instanceof PooledDataBuffer) {
            PooledDataBuffer pooledDataBuffer = (PooledDataBuffer) dataBuffer;
            if (pooledDataBuffer.isAllocated()) {
                if (pooledDataBuffer instanceof NettyDataBuffer) {
                    NettyDataBuffer ndb = (NettyDataBuffer) pooledDataBuffer;
                    ByteBuf nativeBuffer = ndb.getNativeBuffer();
                    int refCnt = nativeBuffer.refCnt();
                    if (refCnt < 1) {
                        if (log.isDebugEnabled()) {
                            log.debug(nativeBuffer + " ref cnt is " + refCnt, LogService.BIZ_ID, reqId);
                        }
                        return false;
                    }
                }
                return pooledDataBuffer.release();
            }
        }
        return false;
    }
}
