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

package we.spring.http.server.reactive.ext;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Mono;
import we.util.NettyDataBufferUtils;

/**
 * @author hongqiaowei
 */

public abstract class FizzServerHttpResponseDecorator extends ServerHttpResponseDecorator {

    public FizzServerHttpResponseDecorator(ServerHttpResponse delegate) {
        super(delegate);
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> bodyPublisher) {

        return
                NettyDataBufferUtils.join(bodyPublisher).defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER)
                                    .flatMap(
                                            body -> {
                                                DataBuffer b = null;
                                                if (body != NettyDataBufferUtils.EMPTY_DATA_BUFFER) {
                                                    if (body instanceof PooledDataBuffer) {
                                                        try {
                                                            b = NettyDataBufferUtils.copy2heap(body);
                                                        } finally {
                                                            NettyDataBufferUtils.release(body);
                                                        }
                                                    } else {
                                                        b = body;
                                                    }
                                                }
                                                Publisher<? extends DataBuffer> r = writeWith(b);
                                                return super.writeWith(r);
                                            }
                                    );
    }

    /**
     * You can getDelegate().getHeaders().set("h", "v") in the method and others for response.
     * @param remoteResponseBody
     * @return the real http response body to client, or Mono.empty() if response without body
     */
    public abstract Publisher<? extends DataBuffer> writeWith(DataBuffer remoteResponseBody);
}
