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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidatorTypeCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JSON Schema工具类
 * @author zhongjie
 */
public class JsonSchemaUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonSchemaUtils.class);
    static {
        try {
            // 替换验证信息提示
            Field messageFormatField = ValidatorTypeCode.class.getDeclaredField("messageFormat");
            //忽略属性的访问权限
            messageFormatField.setAccessible(true);
            messageFormatField.set(ValidatorTypeCode.ADDITIONAL_PROPERTIES, new MessageFormat(
                    "{1}在schema中没有定义并且schema不允许指定外的字段"));
            messageFormatField.set(ValidatorTypeCode.ALL_OF, new MessageFormat("should be valid to all the schemas {1}"));
            messageFormatField.set(ValidatorTypeCode.ANY_OF, new MessageFormat("should be valid to any of the schemas {1}"));
            messageFormatField.set(ValidatorTypeCode.CROSS_EDITS, new MessageFormat("has an error with 'cross edits'"));
            messageFormatField.set(ValidatorTypeCode.DEPENDENCIES, new MessageFormat("has an error with dependencies {1}"));
            messageFormatField.set(ValidatorTypeCode.EDITS, new MessageFormat("has an error with 'edits'"));
            messageFormatField.set(ValidatorTypeCode.ENUM, new MessageFormat("值不在限定{1}内"));
            messageFormatField.set(ValidatorTypeCode.FORMAT, new MessageFormat("不符合{1}格式{2}"));
            messageFormatField.set(ValidatorTypeCode.ITEMS, new MessageFormat("在索引[{1}]处为找到验证器"));
            messageFormatField.set(ValidatorTypeCode.MAXIMUM, new MessageFormat("给定值应当小于等于{1}"));
            messageFormatField.set(ValidatorTypeCode.MAX_ITEMS, new MessageFormat("数组至多含有{1}个元素"));
            messageFormatField.set(ValidatorTypeCode.MAX_LENGTH, new MessageFormat("长度应当最多{1}"));
            messageFormatField.set(ValidatorTypeCode.MAX_PROPERTIES, new MessageFormat("对象最多有{1}个字段"));
            messageFormatField.set(ValidatorTypeCode.MINIMUM, new MessageFormat("给定值应当大于等于{1}"));
            messageFormatField.set(ValidatorTypeCode.MIN_ITEMS, new MessageFormat("数组至少含有{1}个元素"));
            messageFormatField.set(ValidatorTypeCode.MIN_LENGTH, new MessageFormat("长度应当最少{1}"));
            messageFormatField.set(ValidatorTypeCode.MIN_PROPERTIES, new MessageFormat("{0}:对象最少有{1}个字段"));
            messageFormatField.set(ValidatorTypeCode.MULTIPLE_OF, new MessageFormat("数值类型应当是{1}"));
            messageFormatField.set(ValidatorTypeCode.NOT_ALLOWED, new MessageFormat("{1}不允许出现在数据中"));
            messageFormatField.set(ValidatorTypeCode.NOT, new MessageFormat("should not be valid to the schema {1}"));
            messageFormatField.set(ValidatorTypeCode.ONE_OF, new MessageFormat("should be valid to one and only one of the schemas {1}"));
            messageFormatField.set(ValidatorTypeCode.PATTERN_PROPERTIES, new MessageFormat("has some error with 'pattern properties'"));
            messageFormatField.set(ValidatorTypeCode.PATTERN, new MessageFormat("应当符合格式\"{1}\""));
            messageFormatField.set(ValidatorTypeCode.PROPERTIES, new MessageFormat("对象字段存在错误"));
            messageFormatField.set(ValidatorTypeCode.READ_ONLY, new MessageFormat("is a readonly field, it cannot be changed"));
            messageFormatField.set(ValidatorTypeCode.REF, new MessageFormat("has an error with 'refs'"));
            messageFormatField.set(ValidatorTypeCode.REQUIRED, new MessageFormat("{1}字段不能为空"));
            messageFormatField.set(ValidatorTypeCode.TYPE, new MessageFormat("预期类型是{2},但实际是{1}"));
            messageFormatField.set(ValidatorTypeCode.UNION_TYPE, new MessageFormat("预期类型是{2},但实际是{1}"));
            messageFormatField.set(ValidatorTypeCode.UNIQUE_ITEMS, new MessageFormat("数组元素唯一性冲突"));
            messageFormatField.set(ValidatorTypeCode.DATETIME, new MessageFormat("{1}不是一个有效的{2}"));
            messageFormatField.set(ValidatorTypeCode.UUID, new MessageFormat("{1}不是一个有效的{2}"));
            messageFormatField.set(ValidatorTypeCode.ID, new MessageFormat("{1} is an invalid segment for URI {2}"));
            messageFormatField.set(ValidatorTypeCode.EXCLUSIVE_MAXIMUM, new MessageFormat("给定值应当小于{1}"));
            messageFormatField.set(ValidatorTypeCode.EXCLUSIVE_MINIMUM, new MessageFormat("给定值应当大于{1}"));
            messageFormatField.set(ValidatorTypeCode.FALSE, new MessageFormat("Boolean schema false is not valid"));
            messageFormatField.set(ValidatorTypeCode.CONST, new MessageFormat("值应当是一个常量{1}"));
            messageFormatField.set(ValidatorTypeCode.CONTAINS, new MessageFormat("没有包含元素能够通过验证:{1}"));
        } catch (Exception e) {
            LOGGER.warn("替换ValidatorTypeCode.messageFormat异常", e);
        }
    }

    private JsonSchemaUtils() {}

    /**
     * 验证JSON字符串是否符合JSON Schema要求
     * @param jsonSchema JSON Schema
     * @param inputJson JSON字符串
     * @return null：验证通过，List：报错信息列表
     */
    public static List<String> validate(String jsonSchema, String inputJson) {
        return internalValidate(jsonSchema, inputJson, Boolean.FALSE);
    }

    /**
     * 验证JSON字符串是否符合JSON Schema要求，允许数字\布尔类型 是字符串格式
     * @param jsonSchema JSON Schema
     * @param inputJson JSON字符串
     * @return null：验证通过，List：报错信息列表
     */
    public static List<String> validateAllowValueStr(String jsonSchema, String inputJson) {
        return internalValidate(jsonSchema, inputJson, Boolean.TRUE);
    }

    private static List<String> internalValidate(String jsonSchema, String inputJson, boolean typeLoose) {
        CheckJsonResult checkJsonResult = checkJson(jsonSchema, inputJson, typeLoose);
        if (checkJsonResult.errorList != null) {
            return checkJsonResult.errorList;
        }

        Set<ValidationMessage> validationMessageSet = checkJsonResult.schema.validate(checkJsonResult.json);
        if (CollectionUtils.isEmpty(validationMessageSet)) {
            return null;
        }

        return validationMessageSet.stream().map(validationMessage -> {
            String message = validationMessage.getMessage();
            if (message != null) {
                return message;
            }
            return validationMessage.getCode();
        }).collect(Collectors.toList());
    }

    private static CheckJsonResult checkJson(String jsonSchema, String inputJson, boolean typeLoose) {
        CheckJsonResult checkJsonResult = new CheckJsonResult();
        try {
            checkJsonResult.schema = getJsonSchemaFromStringContent(jsonSchema, typeLoose);
        } catch (Exception e) {
            checkJsonResult.errorList = new ArrayList<>(1);
            checkJsonResult.errorList.add(String.format("JSON Schema格式错误，提示信息[%s]", e.getMessage()));
            return checkJsonResult;
        }

        try {
            checkJsonResult.json = getJsonNodeFromStringContent(inputJson);
        } catch (Exception e) {
            checkJsonResult.errorList = new ArrayList<>(1);
            checkJsonResult.errorList.add(String.format("待验证JSON格式错误，提示信息[%s]", e.getMessage()));
            return checkJsonResult;
        }

        return checkJsonResult;
    }

    private static class CheckJsonResult {
        JsonSchema schema;
        JsonNode json;
        List<String> errorList;
    }

    private static final JsonSchemaFactory JSON_SCHEMA_FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    private static final SchemaValidatorsConfig CONFIG_WITH_TYPE_LOOSE;
    private static final SchemaValidatorsConfig CONFIG_WITHOUT_TYPE_LOOSE;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static {
        CONFIG_WITH_TYPE_LOOSE = new SchemaValidatorsConfig();
        CONFIG_WITH_TYPE_LOOSE.setTypeLoose(Boolean.TRUE);
        CONFIG_WITHOUT_TYPE_LOOSE = new SchemaValidatorsConfig();
        CONFIG_WITHOUT_TYPE_LOOSE.setTypeLoose(Boolean.FALSE);
    }
    private static JsonSchema getJsonSchemaFromStringContent(String schemaContent, boolean typeLoose) {
        SchemaValidatorsConfig config = typeLoose ? CONFIG_WITH_TYPE_LOOSE : CONFIG_WITHOUT_TYPE_LOOSE;
        return JSON_SCHEMA_FACTORY.getSchema(schemaContent, config);
    }

    private static JsonNode getJsonNodeFromStringContent(String content) throws Exception {
        return OBJECT_MAPPER.readTree(content);
    }
}
