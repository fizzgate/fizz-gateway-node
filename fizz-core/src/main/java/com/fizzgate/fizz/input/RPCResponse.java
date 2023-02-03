/*
 *  Copyright (C) 2021 the original author or authors.
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

package com.fizzgate.fizz.input;

import org.springframework.util.MultiValueMap;

import reactor.core.publisher.Mono;

/**
 *
 * @author linwaiwai
 *
 */
public class RPCResponse {
    private MultiValueMap headers;
    private Mono<Object> bodyMono;

    public MultiValueMap getHeaders() {
        return headers;
    }

    public void setHeaders(MultiValueMap headers) {
        this.headers = headers;
    }

    public Mono<Object> getBodyMono() {
        return bodyMono;
    }

    public void setBodyMono(Mono<Object> bodyMono) {
        this.bodyMono = bodyMono;
    }
}
