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

package com.fizzgate.util;

import org.junit.jupiter.api.Test;
import com.fizzgate.schema.util.I18nUtils;
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