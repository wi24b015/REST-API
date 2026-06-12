package com.energy.current_percentage_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:current-percentage-service-test;DB_CLOSE_DELAY=-1;NON_KEYWORDS=HOUR",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false"
})
class CurrentPercentageServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
