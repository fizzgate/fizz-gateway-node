package we.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Mills2localDateDeserializer extends JsonDeserializer<LocalDate> {

    public LocalDate deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
        String text = jp.getText();
        if (StringUtils.isBlank(text)) {
            return null;
        }
        LocalDateTime ldt = DateTimeUtils.transform(Long.parseLong(text));
        return ldt.toLocalDate();
    }
}