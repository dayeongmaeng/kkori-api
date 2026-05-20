package com.kkori.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "auth.jwt.secret=test-jwt-secret-test-jwt-secret-1234")
class KkoriApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
