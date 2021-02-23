package we.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author hongqiaowei
 */

public class FizzWebClientTests {

    @Test
    void extractServiceOrAddressTest() {
        FizzWebClient fizzWebClient = new FizzWebClient();

        String r0 = fizzWebClient.extractServiceOrAddress("http://www.baidu.com");
        assertEquals(r0, "www.baidu.com");

        String r1 = fizzWebClient.extractServiceOrAddress("https://www.baidu.com");
        assertEquals(r1, "www.baidu.com");

        String r2 = fizzWebClient.extractServiceOrAddress("https://aservice");
        assertEquals(r2, "aservice");

        String r3 = fizzWebClient.extractServiceOrAddress("https://aservice/ypath");
        assertEquals(r3, "aservice");

        String r4 = fizzWebClient.extractServiceOrAddress("https://127.0.0.1");
        assertEquals(r4, "127.0.0.1");

        String r5 = fizzWebClient.extractServiceOrAddress("https://127.0.0.1:8600");
        assertEquals(r5, "127.0.0.1:8600");

        String r6 = fizzWebClient.extractServiceOrAddress("http://127.0.0.1:8600/apath");
        assertEquals(r6, "127.0.0.1:8600");
    }
}
