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

package we.proxy.grpc.client.utils;

import static com.google.protobuf.util.JsonFormat.TypeRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;

import io.grpc.stub.StreamObserver;
import we.proxy.grpc.client.CallResults;

/**
 * @author zhangjikai
 */
public class MessageWriter<T extends Message> implements StreamObserver<T> {
    private static final Logger logger = LoggerFactory.getLogger(MessageWriter.class);

    private final Printer printer;
    private final CallResults results;

    private MessageWriter(Printer printer, CallResults results) {
        this.printer = printer;
        this.results = results;
    }

    public static <T extends Message> MessageWriter<T> newInstance(TypeRegistry registry, CallResults results){
        return new MessageWriter<>(
                JsonFormat.printer().usingTypeRegistry(registry).includingDefaultValueFields(),
                results);
    }

    @Override
    public void onNext(T value) {
        try {
            results.add(printer.print(value));
        } catch (InvalidProtocolBufferException e) {
            logger.error("Skipping invalid response message", e);
        }
    }

    @Override
    public void onError(Throwable t) {
        logger.error("Messages write occur errors", t);
    }

    @Override
    public void onCompleted() {
        logger.info("Messages write complete");
    }
}
