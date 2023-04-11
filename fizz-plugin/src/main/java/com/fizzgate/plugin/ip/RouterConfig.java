package com.fizzgate.plugin.ip;

import lombok.Data;
import org.springframework.http.MediaType;

@Data
public class RouterConfig {
    private String errorRespContentType = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private String errorRespContent = "{\"msg\":\"非法IP\",\"code\":-1}";
    private String whiteIp;
    private String blackIp;

    public interface FieldName {
        String ERROR_RESP_CONTENT_TYPE = "errorRespContentType";
        String ERROR_RESP_CONTENT = "errorRespContent";
        String WHITE_IP = "whiteIp";
        String BLACK_IP = "blackIp";
    }
}
