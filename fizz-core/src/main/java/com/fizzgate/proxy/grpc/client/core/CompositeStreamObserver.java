/*
Copyright (c) 2016, gRPC Ecosystem
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of polyglot nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.fizzgate.proxy.grpc.client.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.grpc.stub.StreamObserver;

/**
 * A {@link StreamObserver} which groups multiple observers and executes them all.
 */
public class CompositeStreamObserver<T> implements StreamObserver<T> {
    private static final Logger logger = LoggerFactory.getLogger(CompositeStreamObserver.class);
    private final ImmutableList<StreamObserver<T>> observers;

    @SafeVarargs
    public static <T> CompositeStreamObserver<T> of(StreamObserver<T>... observers) {
        return new CompositeStreamObserver<>(ImmutableList.copyOf(observers));
    }

    private CompositeStreamObserver(ImmutableList<StreamObserver<T>> observers) {
        this.observers = observers;
    }

    @Override
    public void onCompleted() {
        for (StreamObserver<T> observer : observers) {
            try {
                observer.onCompleted();
            } catch (Throwable t) {
                logger.error("Exception in composite onComplete, moving on", t);
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        for (StreamObserver<T> observer : observers) {
            try {
                observer.onError(t);
            } catch (Throwable s) {
                logger.error("Exception in composite onError, moving on", s);
            }
        }
    }

    @Override
    public void onNext(T value) {
        for (StreamObserver<T> observer : observers) {
            try {
                observer.onNext(value);
            } catch (Throwable t) {
                logger.error("Exception in composite onNext, moving on", t);
            }
        }
    }
}