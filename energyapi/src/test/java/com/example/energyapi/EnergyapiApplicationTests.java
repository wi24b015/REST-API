package com.example.energyapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:energyapi-test;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"spring.rabbitmq.listener.direct.auto-startup=false",
		"spring.rabbitmq.dynamic=false"
})
class EnergyapiApplicationTests {

	@Test
	void contextLoads() {
	}
}