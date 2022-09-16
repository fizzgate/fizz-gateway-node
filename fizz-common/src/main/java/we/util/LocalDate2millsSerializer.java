package we.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDate;

public class LocalDate2millsSerializer extends JsonSerializer<LocalDate> {

    @Override
    public void serialize(LocalDate ld, JsonGenerator jg, SerializerProvider sp) throws IOException {
        jg.writeNumber(DateTimeUtils.toMillis(ld));
    }
}