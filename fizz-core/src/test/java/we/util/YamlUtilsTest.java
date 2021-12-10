package we.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class YamlUtilsTest {

    @Test
    void dumpJavaObject() throws IOException {
        InetUtilsProperties inetUtilsProperties = new InetUtilsProperties();
        InetUtils inetUtils = new InetUtils(inetUtilsProperties);
        EurekaInstanceConfigBean eurekaInstanceConfigBean = new EurekaInstanceConfigBean(inetUtils);
        eurekaInstanceConfigBean.setAppname("abc");

        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("md1", "val1");
        eurekaInstanceConfigBean.setMetadataMap(metadataMap);

        MyDataCenterInfo myDataCenterInfo = new MyDataCenterInfo(DataCenterInfo.Name.MyOwn);
        eurekaInstanceConfigBean.setDataCenterInfo(myDataCenterInfo);

//        Yaml yaml = new Yaml();
//        StringWriter writer = new StringWriter();
//        yaml.dump(eurekaInstanceConfigBean, writer);
//        System.out.println(writer.toString());

//        Yaml yaml = new Yaml();
//        String output = yaml.dump(eurekaInstanceConfigBean);
//        System.out.println(output);

        String s = FileUtil.readString("test.yml", CharsetUtil.CHARSET_UTF_8);
        System.out.println("------------------------------------" + s + "------------------------------------");
    }
}
