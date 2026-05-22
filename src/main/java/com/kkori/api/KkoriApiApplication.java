package com.kkori.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class KkoriApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(KkoriApiApplication.class, args);
	}

}
