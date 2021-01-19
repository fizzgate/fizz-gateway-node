package we.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import we.controller.FlowControlController;
import we.util.JacksonUtils;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author hongqiaowei
 */

public class FlowControlFilterTests {

    @Test
    void test() throws InterruptedException {
        WebTestClient client = WebTestClient.bindToController(new FlowControlController()).build();
        client.get().uri("/admin/flowStat/globalConcurrentsRps")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType("application/json;charset=UTF-8")
                    // .expectBody().json("{\"concurrents\":0, \"rps\":0}")
                    .expectBody(String.class).value(
                                                    v -> {
                                                        HashMap<String, Integer> m = JacksonUtils.readValue(v, HashMap.class);
                                                        assertEquals(m.get("concurrents"), 0);
                                                    }
                                             )
                    ;
        Thread.sleep(3000);
    }
}
