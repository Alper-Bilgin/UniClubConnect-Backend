package com.uniclubconnect.infrastructure.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableConfigServer     // Bu servisin bir Config Server olduğunu belirtir
@EnableDiscoveryClient  // Bu servisin Eureka'ya kayıt olacağını belirtir
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}