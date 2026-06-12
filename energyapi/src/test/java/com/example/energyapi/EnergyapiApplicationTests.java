package com.example.energyapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:energyapi-test;DB_CLOSE_DELAY=-1;NON_KEYWORDS=HOUR",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=validate"
})
class EnergyapiApplicationTests {

	@Test
	void contextLoads() {
	}
}
