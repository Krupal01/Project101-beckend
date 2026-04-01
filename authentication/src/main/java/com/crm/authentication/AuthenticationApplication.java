package com.crm.authentication;

import com.crm.authentication.configure.JwtConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(JwtConfig.class)
@SpringBootApplication
public class AuthenticationApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthenticationApplication.class, args);
	}

}
