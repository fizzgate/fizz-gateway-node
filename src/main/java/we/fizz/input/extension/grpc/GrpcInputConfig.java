package we.fizz.input.extension.grpc;

import we.fizz.input.InputConfig;

import java.util.Map;

public class GrpcInputConfig extends InputConfig {
    private int timeout;
    private String serviceName;
    private String method;
    public GrpcInputConfig(Map configMap)  {
        super(configMap);
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
