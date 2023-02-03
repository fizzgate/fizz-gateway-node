package com.fizzgate.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;

public class Mills2localDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    public LocalDateTime deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
        String text = jp.getText();
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return DateTimeUtils.transform(Long.parseLong(text));
    }
}