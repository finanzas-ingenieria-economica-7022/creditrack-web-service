package com.creditrack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class CrediTrackApplicationTests {

    @Test
    public void contextLoads() {
        // Verifies that the multi-datasource Spring ApplicationContext compiles and boots up successfully.
    }
}
