package we;

import ognl.Ognl;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OgnlTests {

    @Test
    void testGet() throws Exception {

        Root root = new Root();

        Map<String, Object> query = new HashMap<>();
        query.put("version", "v2");
        query.put("userId", 1234563);
        query.put("age", 25);

        root.put("query", query);

        Map<String, Object> client = new HashMap<>();
        client.put("ip", "10.2.3.4");
        client.put("ip2", "10.2.3.88");
        root.put("client", client);

        Boolean result = (Boolean) Ognl.getValue("checkIp(client.ip2) && (query.version == 'v2' || query.age < 20) and query.age in (22,25,30) && client.ip=='10.2.3.4'", root);

        System.out.println(result);
        assertEquals(true, result);
    }
}

class Root extends HashMap {

    public Root() {
    }

    public boolean checkIp(String ip) {
        System.out.println(ip);
        return ip.equals("10.2.3.88");
    }

}