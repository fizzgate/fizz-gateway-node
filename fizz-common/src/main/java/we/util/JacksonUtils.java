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

package we.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import we.util.Consts.DP;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author hongqiaowei
 */

public abstract class JacksonUtils {

    private static ObjectMapper m;

    static {
        JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES,        true);
        f.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        m = new ObjectMapper(f);

        m.setSerializationInclusion(Include.NON_EMPTY);
        m.configure(                SerializationFeature.   WRITE_ENUMS_USING_TO_STRING,  true);
        m.configure(                SerializationFeature.   WRITE_EMPTY_JSON_ARRAYS,      true); // FIXME
        m.configure(                SerializationFeature.   WRITE_NULL_MAP_VALUES,        true);
        m.configure(                DeserializationFeature. READ_ENUMS_USING_TO_STRING,   true);
        m.configure(                DeserializationFeature. FAIL_ON_NUMBERS_FOR_ENUMS,    true);
        m.configure(                DeserializationFeature. FAIL_ON_UNKNOWN_PROPERTIES,   false);
        m.configure(                JsonParser.Feature.     ALLOW_UNQUOTED_CONTROL_CHARS, true);

        SimpleModule m0 = new SimpleModule();
        m0.addDeserializer(Date.class, new DateDeseralizer());
        m.registerModule(m0);

        SimpleModule m1 = new SimpleModule();
        m1.addDeserializer(LocalDate.class, new LocalDateDeseralizer());
        m.registerModule(m1);

        SimpleModule m2 = new SimpleModule();
        m2.addDeserializer(LocalDateTime.class, new LocalDateTimeDeseralizer());
        m.registerModule(m2);

        SimpleModule m3 = new SimpleModule();
        m3.addSerializer(LocalDateTime.class, new LocalDateTimeSeralizer());
        m.registerModule(m3);
    }

    private JacksonUtils() {
    }

    public static ObjectMapper getObjectMapper() {
        return m;
    }

    public static <T> T readValue(String json, Class<T> clz) {
        try {
            return m.readValue(json, clz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readValue(byte[] bytes, Class<T> clz) {
        try {
            return m.readValue(bytes, clz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String writeValueAsString(Object value) {
        try {
            return m.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] writeValueAsBytes(Object value) {
        try {
            return m.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

class DateDeseralizer extends JsonDeserializer<Date> {

    public Date deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {

        String s = jp.getText();
        int sl = s.length();
        if (sl == DP.MILLS_LEN) {
            return new Date(Long.parseLong(s));
        } else {
            String dtp = DP.DP10;
            DateTimeFormatter dtf = null;
            if (sl == DP.DP10.length()) {
            } else if (sl == DP.DP14.length()) {
                dtp = DP.DP14;
            } else if (sl == DP.DP19.length()) {
                dtp = DP.DP19;
            } else if (sl == DP.DP23.length()) {
                dtp = DP.DP23;
            } else {
                throw new IOException("invalid datetime pattern: " + s);
            }
            dtf = DateTimeUtils.getDateTimeFormatter(dtp);
            LocalDateTime ldt = LocalDateTime.parse(s, dtf);
            return DateTimeUtils.from(ldt);
        }
    }
}

class LocalDateDeseralizer extends JsonDeserializer<LocalDate> {

    public LocalDate deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {

        String s = jp.getText();
        if (s.length() == DP.DP10.length()) {
            DateTimeFormatter dtf = DateTimeUtils.getDateTimeFormatter(DP.DP10);
            return LocalDate.parse(s, dtf);
        } else {
            throw new IOException("invalid datetime pattern: " + s);
        }
    }
}

class LocalDateTimeDeseralizer extends JsonDeserializer<LocalDateTime> {

    public LocalDateTime deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {

        String s = jp.getText();
        int sl = s.length();
        if (sl == DP.MILLS_LEN) {
            return DateTimeUtils.transform(Long.parseLong(s));
        } else {
            String dtp = DP.DP10;
            DateTimeFormatter dtf = null;
            if (sl == DP.DP10.length()) {
            } else if (sl == DP.DP14.length()) {
                dtp = DP.DP14;
            } else if (sl == DP.DP19.length()) {
                dtp = DP.DP19;
            } else if (sl == DP.DP23.length()) {
                dtp = DP.DP23;
            } else {
                throw new IOException("invalid datetime pattern: " + s);
            }
            dtf = DateTimeUtils.getDateTimeFormatter(dtp);
            return LocalDateTime.parse(s, dtf);
        }
    }
}

class LocalDateTimeSeralizer extends JsonSerializer<LocalDateTime> {

    @Override
    public void serialize(LocalDateTime ldt, JsonGenerator jg, SerializerProvider sp) throws IOException {
        jg.writeNumber(DateTimeUtils.toMillis(ldt));
    }
}
