package we.proxy.dubbo;

public class DubboInterfaceDeclaration {
    private String parameterTypes;
    private String method;
    private String serviceName;
    private int timeout;

    public DubboInterfaceDeclaration() {
    }


    public String getParameterTypes() {
        return parameterTypes;
    }
    // call method name
    public String getMethod() {
        return method;
    }
    // service name
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setParameterTypes(String parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Integer getTimeout() {
        return this.timeout;
    }
}
