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

package com.fizzgate.util;

import io.netty.buffer.ByteBufAllocator;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;

import java.nio.charset.StandardCharsets;

/**
 * @author hongqiaowei
 */

public abstract class NettyDataBufferUtils extends org.springframework.core.io.buffer.DataBufferUtils {

    private static NettyDataBufferFactory dataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

    public static final DataBuffer EMPTY_DATA_BUFFER = from(new byte[0]);

    private NettyDataBufferUtils() {
    }

    public static NettyDataBuffer from(String s) {
        return from(s.getBytes(StandardCharsets.UTF_8));
    }

    public static NettyDataBuffer from(byte[] bytes) {
        return (NettyDataBuffer) dataBufferFactory.wrap(bytes);
    }

    public static byte[] copyBytes(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        return bytes;
    }

    public static DataBuffer copy2heap(DataBuffer dataBuffer) {
        return from(copyBytes(dataBuffer));
    }
}
