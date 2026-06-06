package com.wpanther.pisp.fee.ai;

import com.wpanther.pisp.fee.ai.support.PostgresTestSupport;
import com.wpanther.pisp.fee.ai.support.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestSecurityConfig.class)
class ContextLoadsTest extends PostgresTestSupport {
    @Test
    void contextLoads() {
    }
}
