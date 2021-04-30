package we.util;

import org.junit.jupiter.api.Test;
import we.schema.util.I18nUtils;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaUtilsTest {

    @Test
    void validateRequiredPropertyWithoutAssignedTitleAndTitleEn() {
        I18nUtils.setContextLocale(new Locale("zh"));
        try {
            List<String> validateList = JsonSchemaUtils.validate(
                    "{\n" +
                            "    \"properties\": {\n" +
                            "        \"library\": {\n" +
                            "            \"type\": \"object\",\n" +
                            "            \"required\": [\n" +
                            "                \"person\"\n" +
                            "            ],\n" +
                            "            \"properties\": {\n" +
                            "                \"person\": {\n" +
                            "                    \"type\": \"string\"\n" +
                            "                }\n" +
                            "            }\n" +
                            "        }\n" +
                            "    },\n" +
                            "    \"required\": [\n" +
                            "        \"library\"\n" +
                            "    ],\n" +
                            "    \"type\": [\n" +
                            "        \"object\",\n" +
                            "        \"null\"\n" +
                            "    ]\n" +
                            "}",
                    "{\n" +
                            "    \"library\":{\n" +
                            "    }\n" +
                            "}");
            assertNotNull(validateList);
            assertEquals(1, validateList.size());
            assertEquals("person不能为空", validateList.get(0));
        } finally {
            I18nUtils.removeContextLocale();
        }
    }
}