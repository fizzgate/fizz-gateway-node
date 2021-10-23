package we.api.pairing;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import we.Fizz;
import we.redis.RedisProperties;
import we.redis.RedisServerConfiguration;
import we.redis.RedisTemplateConfiguration;
import we.util.ReflectionUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author hongqiaowei
 */

@TestPropertySource("/application.properties")
@SpringJUnitConfig(classes = {RedisProperties.class, RedisTemplateConfiguration.class, RedisServerConfiguration.class})
public class ApiPairingDocSetServiceTests {

    @Resource
    StringRedisTemplate         stringRedisTemplate;

    @Resource
    ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    ApiPairingDocSetService     apiPairingDocSetService;

    @BeforeEach
    void beforeEach() throws NoSuchFieldException {
        apiPairingDocSetService = new ApiPairingDocSetService();
        ReflectionUtils.set(apiPairingDocSetService, "rt", reactiveStringRedisTemplate);
    }

    @Test
    void initTest() throws Throwable {

        Fizz.context = new GenericApplicationContext();
        Fizz.context.refresh();

        Map<String, String> hash = new HashMap<>();
        hash.put("c", "{\"id\":1,\"name\":\"DocSet1\",\"docs\":[{\"service\":\"we-meb\",\"apis\":[{\"method\":\"GET\",\"path\":\"/getMebInfo\"}]}],\"appIds\":[\"app1\"]}");
        hash.put("d", "{\"id\":2,\"name\":\"DocSet2\",\"docs\":[{\"service\":\"we-meb\",\"apis\":[{\"method\":\"GET\",\"path\":\"/getMebInfo\"}]}],\"appIds\":[\"app1\"]}");
//      hash.put("a", "{\"isDeleted\":1,\"id\":1,\"name\":\"DocSet1\",\"docs\":[{\"service\":\"we-meb\",\"apis\":[{\"method\":\"GET\",\"path\":\"/getMebInfo\"}]}],\"appIds\":[\"app1\"]}");
//      hash.put("b", "{\"isDeleted\":1,\"id\":2,\"name\":\"DocSet2\",\"docs\":[{\"service\":\"we-meb\",\"apis\":[{\"method\":\"GET\",\"path\":\"/getMebInfo\"}]}],\"appIds\":[\"app1\"]}");
        stringRedisTemplate.opsForHash().putAll("fizz_api_pairing_doc", hash);

        apiPairingDocSetService.init();
        Map<Integer, ApiPairingDocSet> docSetMap = apiPairingDocSetService.getDocSetMap();
        Map<String, Set<ApiPairingDocSet>> appDocSetMap = apiPairingDocSetService.getAppDocSetMap();
        Map<String, Set<ApiPairingDocSet>> serviceExistsInDocSetMap = apiPairingDocSetService.getServiceExistsInDocSetMap();
        Map<String, Map<Object, Set<ApiPairingDocSet>>> pathMethodExistsInDocSetMap = apiPairingDocSetService.getPathMethodExistsInDocSetMap();
//      System.err.println("docSetMap: " + JacksonUtils.writeValueAsString(docSetMap));
//      System.err.println("appDocSetMap: " + JacksonUtils.writeValueAsString(appDocSetMap));
//      System.err.println("serviceExistsInDocSetMap: " + JacksonUtils.writeValueAsString(serviceExistsInDocSetMap));
//      System.err.println("pathMethodExistsInDocSetMap: " + JacksonUtils.writeValueAsString(pathMethodExistsInDocSetMap));

        boolean b = apiPairingDocSetService.existsDocSetMatch("app1", HttpMethod.GET, "we-meb", "/getMebInfo");
        Assert.assertTrue(b);
    }
}
