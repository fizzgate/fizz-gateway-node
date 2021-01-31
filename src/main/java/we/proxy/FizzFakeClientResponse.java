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

package we.proxy;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author hongqiaowei
 */

class FizzFakeClientResponse implements ClientResponse {

    @Override
    public ExchangeStrategies strategies() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpStatus statusCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int rawStatusCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Headers headers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MultiValueMap<String, ResponseCookie> cookies() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Void> releaseBody() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<ResponseEntity<Void>> toBodilessEntity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeReference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<WebClientResponseException> createException() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String logPrefix() {
        throw new UnsupportedOperationException();
    }

}
