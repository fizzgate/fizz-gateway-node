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

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;
import we.util.NettyDataBufferUtils;

import java.nio.charset.StandardCharsets;

/**
 * @author hongqiaowei
 */

public class FizzServerHttpRequestDecorator extends ServerHttpRequestDecorator {

    private HttpHeaders headers;

    private Flux<DataBuffer> body = Flux.empty();

    public FizzServerHttpRequestDecorator(ServerHttpRequest delegate) {
        super(delegate);
        headers = HttpHeaders.writableHttpHeaders(delegate.getHeaders());
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setBody(DataBuffer body) {
        if (body instanceof PooledDataBuffer) {
            byte[] bytes = new byte[body.readableByteCount()];
            body.read(bytes);
            setBody(bytes);
        } else {
            this.body = Flux.just(body);
        }
    }

    public void setBody(String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        setBody(bytes);
    }

    public void setBody(byte[] body) {
        NettyDataBuffer from = NettyDataBufferUtils.from(body);
        this.body = Flux.just(from);
    }

    @Override
    public Flux<DataBuffer> getBody() {
        return body;
    }

    public DataBuffer getRawBody() {
        final DataBuffer[] raw = {null};
        body.subscribe(
                dataBuffer -> {
                    raw[0] = dataBuffer;
                }
        );
        return raw[0];
    }
}
