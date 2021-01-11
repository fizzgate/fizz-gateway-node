package we.fizz.input;

import org.springframework.util.StringUtils;
import we.fizz.exception.FizzException;
import we.fizz.exception.FizzRuntimeException;

import java.util.Map;

public class DubboInputConfig extends InputConfig {
    private String serviceName;
    private String method;

    public String getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(String parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    private String parameterTypes;

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMethod() {
        return method;
    }





    public DubboInputConfig(Map configMap)  {
        super(configMap);
    }
    public void parse(){
        String serviceName = (String) configMap.get("serviceName");
        if(StringUtils.isEmpty(serviceName)) {
            throw new FizzRuntimeException("service name can not be blank");
        }
        setServiceName(serviceName);
        String method = (String) configMap.get("method");
        if (StringUtils.isEmpty(method)) {
            throw new FizzRuntimeException("method can not be blank");
        }
        setMethod(method);
    }

    private int timeout;
    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }


}
