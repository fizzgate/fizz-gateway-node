package com.fizzgate.fizz;

import org.junit.jupiter.api.Test;
import org.noear.snack.ONode;

import com.fizzgate.fizz.Pipeline;
import com.fizzgate.fizz.input.ClientInputConfig;
import com.fizzgate.fizz.input.Input;

import com.fizzgate.schema.util.I18nUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * aggregate input validate tests
 *
 * @author zhongjie
 */
class InputValidateTests {
    private Map<String, Object> jsonSchemaDef = ONode.load("{\n" +
            "  \"properties\": {\n" +
            "    \"library\": {\n" +
            "      \"type\": \"string\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\n" +
            "    \"library\"\n" +
            "  ],\n" +
            "  \"type\": [\n" +
            "    \"object\",\n" +
            "    \"null\"\n" +
            "  ]\n" +
            "}").toObject(Map.class);

    @Test
    void inputValidateHeaderTipTest() {
        Pipeline pipeline = new Pipeline();

        ClientInputConfig clientInputConfig = new ClientInputConfig();
        clientInputConfig.setHeadersDef(jsonSchemaDef);
        Input input = new Input();
        input.setConfig(clientInputConfig);
        Map<String, Object> clientInput = new HashMap<>(16);
        clientInput.put("headers", new HashMap<>(0));
        String validateMsg = pipeline.inputValidate(input, clientInput);
        assertNotNull(validateMsg);
        assertTrue(validateMsg.startsWith(Pipeline.ValidateType.HEADER.tipZh));

        I18nUtils.setContextLocale(new Locale("en"));
        try {
            validateMsg = pipeline.inputValidate(input, clientInput);
            assertNotNull(validateMsg);
            assertTrue(validateMsg.startsWith(Pipeline.ValidateType.HEADER.tipEn));
        } finally {
            I18nUtils.removeContextLocale();
        }
    }

    @Test
    void inputValidateQueryParamTipTest() {
        Pipeline pipeline = new Pipeline();

        ClientInputConfig clientInputConfig = new ClientInputConfig();
        clientInputConfig.setParamsDef(jsonSchemaDef);
        Input input = new Input();
        input.setConfig(clientInputConfig);
        Map<String, Object> clientInput = new HashMap<>(16);
        clientInput.put("params", new HashMap<>(0));
        String validateMsg = pipeline.inputValidate(input, clientInput);
        assertNotNull(validateMsg);
        assertTrue(validateMsg.startsWith(Pipeline.ValidateType.QUERY_PARAM.tipZh));

        I18nUtils.setContextLocale(new Locale("en"));
        try {
            validateMsg = pipeline.inputValidate(input, clientInput);
            assertNotNull(validateMsg);
            assertTrue(validateMsg.startsWith(Pipeline.ValidateType.QUERY_PARAM.tipEn));
        } finally {
            I18nUtils.removeContextLocale();
        }
    }

    @Test
    void inputValidateBodyTipTest() {
        Pipeline pipeline = new Pipeline();

        ClientInputConfig clientInputConfig = new ClientInputConfig();
        clientInputConfig.setBodyDef(jsonSchemaDef);
        Input input = new Input();
        input.setConfig(clientInputConfig);
        Map<String, Object> clientInput = new HashMap<>(16);
        clientInput.put("body", new HashMap<>(0));
        String validateMsg = pipeline.inputValidate(input, clientInput);
        assertNotNull(validateMsg);
        assertTrue(validateMsg.startsWith(Pipeline.ValidateType.BODY.tipZh));

        I18nUtils.setContextLocale(new Locale("en"));
        try {
            validateMsg = pipeline.inputValidate(input, clientInput);
            assertNotNull(validateMsg);
            assertTrue(validateMsg.startsWith(Pipeline.ValidateType.BODY.tipEn));
        } finally {
            I18nUtils.removeContextLocale();
        }
    }

    @Test
    void inputValidateScriptTipTest() {
        Pipeline pipeline = new Pipeline();

        ClientInputConfig clientInputConfig = new ClientInputConfig();
        Map<String, Object> scriptValidate = new HashMap<>(4);
        scriptValidate.put("type", "groovy");
        scriptValidate.put("source", "import java.util.List; import java.util.ArrayList; " +
                "List<String> errorList = new ArrayList<>(1); errorList.add(\"same thing error\"); return errorList;");
        clientInputConfig.setScriptValidate(scriptValidate);
        Input input = new Input();
        input.setConfig(clientInputConfig);
        Map<String, Object> clientInput = new HashMap<>(16);
        String validateMsg = pipeline.inputValidate(input, clientInput);
        assertNotNull(validateMsg);
        assertTrue(validateMsg.startsWith(Pipeline.ValidateType.SCRIPT.tipZh));

        I18nUtils.setContextLocale(new Locale("en"));
        try {
            validateMsg = pipeline.inputValidate(input, clientInput);
            assertNotNull(validateMsg);
            assertTrue(validateMsg.startsWith(Pipeline.ValidateType.SCRIPT.tipEn));
        } finally {
            I18nUtils.removeContextLocale();
        }
    }
}