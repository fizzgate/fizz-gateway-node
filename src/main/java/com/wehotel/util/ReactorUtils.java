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
package com.wehotel.util;

import reactor.core.publisher.Mono;

/**
 * @author lancer
 */

public interface ReactorUtils {

    static final Object        OBJ              = new Object();

    static final Object        NULL             = OBJ;

    static final Mono<Object>  INITIATE         = Mono.just(NULL);

    static final Mono<Object>  EMPTY_ASYNC_TASK = INITIATE;

    static final Throwable     EMPTY_THROWABLE  = new Throwable(null, null, false, false) {};
}
