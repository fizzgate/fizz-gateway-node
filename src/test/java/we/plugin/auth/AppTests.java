package we.plugin.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author hongqiaowei
 */

public class AppTests {

    @Test
    void ipWhiteListTest() {
        App app = new App();
        app.setIps("10.237.148.107,10.237.148.134,172.25.33.*,172.25.63.*,172.25.102.*,172.25.104.136-138");
        System.out.println("app: " + app);
        boolean allow = app.allow("10.237.148.107");
                allow = app.allow("10.237.148.134");

                allow = app.allow("172.25.102.2");
                allow = app.allow("172.25.102.254");
                allow = app.allow("172.25.102.3");
                allow = app.allow("172.25.102.251");
                allow = app.allow("172.25.102.249");
                allow = app.allow("172.25.102.138");
                allow = app.allow("172.25.102.22");

                allow = app.allow("172.25.104.136");
                allow = app.allow("172.25.104.137");
                allow = app.allow("172.25.104.138");

        assertTrue(allow);
    }
}
