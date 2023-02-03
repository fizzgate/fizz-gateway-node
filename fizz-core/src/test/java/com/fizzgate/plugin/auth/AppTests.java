package com.fizzgate.plugin.auth;

import org.junit.jupiter.api.Test;

import com.fizzgate.plugin.auth.App;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author hongqiaowei
 */

public class AppTests {

    @Test
    void ipWhiteListTest() {
        App app = new App();
        app.setIps("10.237.148.107,10.237.148.134,172.25.33.*,172.25.63.*,172.25.102.*,172.25.104.136-138," +
                "101.236.11.34-37," +
                "101.236.11.50-53");
        System.out.println("app: " + app);
        boolean allow = app.allow("10.237.148.107");
                assertTrue(allow);
                allow = app.allow("10.237.148.134");
                assertTrue(allow);

                allow = app.allow("172.25.102.1");
                assertTrue(allow);
                allow = app.allow("172.25.102.255");
                assertTrue(allow);
                allow = app.allow("172.25.102.3");
                assertTrue(allow);
                allow = app.allow("172.25.102.251");
                assertTrue(allow);
                allow = app.allow("172.25.102.249");
                assertTrue(allow);
                allow = app.allow("172.25.102.138");
                assertTrue(allow);
                allow = app.allow("172.25.102.22");
                assertTrue(allow);

                allow = app.allow("172.25.104.136");
                assertTrue(allow);
                allow = app.allow("172.25.104.137");
                assertTrue(allow);
                allow = app.allow("172.25.104.138");
                assertTrue(allow);

                allow = app.allow("101.236.11.34");
                assertTrue(allow);
                allow = app.allow("101.236.11.35");
                assertTrue(allow);
                allow = app.allow("101.236.11.36");
                assertTrue(allow);
                allow = app.allow("101.236.11.37");
                assertTrue(allow);

                allow = app.allow("101.236.11.50");
                assertTrue(allow);
                allow = app.allow("101.236.11.51");
                assertTrue(allow);
                allow = app.allow("101.236.11.52");
                assertTrue(allow);
                allow = app.allow("101.236.11.53");
                assertTrue(allow);
    }
}
