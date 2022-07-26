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

import io.netty.handler.codec.http.cookie.Cookie;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.netty.http.server.HttpServerRequest;
import we.util.NettyDataBufferUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author hongqiaowei
 */

public class FizzServerHttpRequestDecorator extends ServerHttpRequestDecorator {

    private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

    private AbstractServerHttpRequest         delegate;

    private HttpServerRequest                 nativeRequest;

    @Nullable
    private MultiValueMap<String, String>     queryParams;

    private HttpHeaders                       headers;

    @Nullable
    private MultiValueMap<String, HttpCookie> cookies;

    private Flux<DataBuffer>                  body            = Flux.empty();

    private byte[]                            bodyBytes;

    public FizzServerHttpRequestDecorator(ServerHttpRequest delegate) {
        super(delegate);
        this.delegate = (AbstractServerHttpRequest) delegate;
        try {
            nativeRequest = this.delegate.getNativeRequest();
        } catch (IllegalStateException e) {
        }
    }

    @Override
    public MultiValueMap<String, String> getQueryParams() {
        if (queryParams == null) {
            queryParams = initQueryParams();
        }
        return queryParams;
    }

    private MultiValueMap<String, String> initQueryParams() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        String query = getURI().getRawQuery();
        if (query != null) {
            Matcher matcher = QUERY_PATTERN.matcher(query);
            while (matcher.find()) {
                String name  = decodeQueryParam(matcher.group(1));
                String eq    = matcher.group(2);
                String value = matcher.group(3);
                value = (value != null ? decodeQueryParam(value) : (StringUtils.hasLength(eq) ? "" : null));
                queryParams.add(name, value);
            }
        }
        return queryParams;
    }

    private String decodeQueryParam(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return URLDecoder.decode(value);
        }
    }

    @Override
    public HttpHeaders getHeaders() {
        if (headers == null) {
            headers = HttpHeaders.writableHttpHeaders(delegate.getHeaders());
        }
        return headers;
    }

    @Override
    public MultiValueMap<String, HttpCookie> getCookies() {
        if (cookies == null) {
            cookies = initCookies();
        }
        return cookies;
    }

    private MultiValueMap<String, HttpCookie> initCookies() {
        if (nativeRequest == null) {
            return null;
        }
        MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
        for (CharSequence name : nativeRequest.cookies().keySet()) {
            for (Cookie cookie : nativeRequest.cookies().get(name)) {
                HttpCookie httpCookie = new HttpCookie(name.toString(), cookie.value());
                cookies.add(name.toString(), httpCookie);
            }
        }
        return cookies;
    }

    public void setEmptyBody() {
        body = Flux.empty();
        bodyBytes = null;
    }

    public void setBody(DataBuffer body) {
        if (body instanceof PooledDataBuffer) {
            byte[] bytes = NettyDataBufferUtils.copyBytes(body);
            setBody(bytes);
        } else {
            this.body = Flux.just(body);
            ByteBuffer byteBuffer = body.asByteBuffer();
            if (byteBuffer.hasArray()) {
                bodyBytes = byteBuffer.array();
            }
        }
    }

    public void setBody(String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        setBody(bytes);
    }

    public void setBody(byte[] body) {
        bodyBytes = body;
        NettyDataBuffer from = NettyDataBufferUtils.from(bodyBytes);
        this.body = Flux.just(from);
    }

    @Override
    public Flux<DataBuffer> getBody() {
        return body;
    }

    public byte[] getBodyBytes() {
        return bodyBytes;
    }
}
