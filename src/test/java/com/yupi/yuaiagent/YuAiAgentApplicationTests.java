package com.yupi.yuaiagent;
import org.mybatis.spring.annotation.MapperScan;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@MapperScan("com.yupi.yuaiagent.mapper")
@SpringBootTest
class YuAiAgentApplicationTests {

    @Test
    void contextLoads() {
    }

}
