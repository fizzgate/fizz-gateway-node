package we.stats.ratelimit;

import we.util.JacksonUtils;

/**
 * @author hongqiaowei
 */

public class ResourceRateLimitConfig {

    public static interface Type {
        static final byte GLOBAL          = 1;
        static final byte SERVICE_DEFAULT = 2;
        static final byte SERVICE         = 3;
        static final byte API             = 4;
    }

    public  static final int    DELETED         = 1;

    public  static final String GLOBAL          = "_global";

    public  static final String SERVICE_DEFAULT = "service_default";

    private static final int    ENABLE          = 1;

    private static final int    UNABLE          = 0;

    public  int     isDeleted = 0;

    public  int     id;

    private boolean enable = true;

    public  String  resource;

    public  byte    type;

    public  long    qps;

    public  long    concurrents;

    public  String  responseType;

    public  String  responseContent;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(int v) {
        if (v == ENABLE) {
            enable = true;
        } else {
            enable = false;
        }
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
