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

package we.spring.web.server.ext;

import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import reactor.core.publisher.Mono;
import we.spring.http.server.reactive.ext.FizzServerHttpRequestDecorator;
import we.util.Consts;
import we.util.NettyDataBufferUtils;
import we.util.ThreadContext;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author hongqiaowei
 */

public class FizzServerWebExchangeDecorator extends ServerWebExchangeDecorator {

    private static final MultiValueMap<String, String> EMPTY_FORM_DATA = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<String, String>(0));

    private static final Mono<MultiValueMap<String, String>> EMPTY_FORM_DATA_MONO = Mono.just(EMPTY_FORM_DATA).cache();

    public FizzServerWebExchangeDecorator(ServerWebExchange delegate) {
        super(delegate);
    }

    private Charset getMediaTypeCharset(@Nullable MediaType mediaType) {
        if (mediaType != null && mediaType.getCharset() != null) {
            return mediaType.getCharset();
        } else {
            return StandardCharsets.UTF_8;
        }
    }

    private MultiValueMap<String, String> parseFormData(Charset charset, String source) {
        String[] pairs = StringUtils.tokenizeToStringArray(source, "&");
        MultiValueMap<String, String> result = new LinkedMultiValueMap<>(pairs.length);
        try {
            for (String pair : pairs) {
                int idx = pair.indexOf('=');
                if (idx == -1) {
                    result.add(URLDecoder.decode(pair, charset.name()), null);
                } else {
                    String name  = URLDecoder.decode(pair.substring(0, idx),  charset.name());
                    String value = URLDecoder.decode(pair.substring(idx + 1), charset.name());
                    result.add(name, value);
                }
            }
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
        return result;
    }

    @Override
    public Mono<MultiValueMap<String, String>> getFormData() {
        ServerHttpRequest req = getDelegate().getRequest();
        MediaType ct = req.getHeaders().getContentType();
        if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(ct)) {
            Charset charset = getMediaTypeCharset(ct);
            return
                    req.getBody().defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER)
                                 .single()
                                 .map(
                                         body -> {
                                             if (body == NettyDataBufferUtils.EMPTY_DATA_BUFFER) {
                                                 return EMPTY_FORM_DATA;
                                             } else {
                                                 CharBuffer charBuffer = charset.decode(body.asByteBuffer());
                                                 return parseFormData(charset, charBuffer.toString());
                                             }
                                         }
                                 );
        } else {
            return EMPTY_FORM_DATA_MONO;
        }
    }

    /**
     * @param dataMap can be {@link org.springframework.util.LinkedMultiValueMap}
     */
    public void setFormData(MultiValueMap<String, String> dataMap) {
        FizzServerHttpRequestDecorator req = (FizzServerHttpRequestDecorator) getDelegate().getRequest();
        req.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        if (CollectionUtils.isEmpty(dataMap)) {
            req.setEmptyBody();
            return;
        }
        StringBuilder b = ThreadContext.getStringBuilder();
        Set<Map.Entry<String, List<String>>> fieldValuesEntries = dataMap.entrySet();
        int fs = fieldValuesEntries.size(), cnt = 0;
        try {
            for (Map.Entry<String, List<String>> fieldValuesEntry : fieldValuesEntries) {
                String field = fieldValuesEntry.getKey();
                List<String> values = fieldValuesEntry.getValue();
                if (CollectionUtils.isEmpty(values)) {
                    b.append(URLEncoder.encode(field, Consts.C.UTF8));
                } else {
                    int vs = values.size();
                    for (int i = 0; i < vs; ) {
                        b.append(URLEncoder.encode(field,         Consts.C.UTF8))
                         .append('=')
                         .append(URLEncoder.encode(values.get(i), Consts.C.UTF8));
                        if ((++i) != vs) {
                            b.append('&');
                        }
                    }
                }
                if ((++cnt) != fs) {
                    b.append('&');
                }
            }
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
        req.setBody(b.toString());
    }
}
