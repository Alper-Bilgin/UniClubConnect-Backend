package com.uniclubconnect.services.gamificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class GamificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GamificationServiceApplication.class, args);
	}

}
