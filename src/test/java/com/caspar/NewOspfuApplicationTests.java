package com.caspar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jwt.secret=Rk9SX1RFU1RfT05MWV9TRUNSRVRfS0VZXzMyX0JZVEVTX0JBU0U2NA==",
        "spring.ai.dashscope.api-key=test-key"
})
class NewOspfuApplicationTests {

    @Test
    void contextLoads() {
    }

}
