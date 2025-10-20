package com.uniclubconnect.services.authservice;

import com.uniclubconnect.services.authservice.entity.ERole;
import com.uniclubconnect.services.authservice.entity.Role;
import com.uniclubconnect.services.authservice.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

@SpringBootApplication
@EnableDiscoveryClient
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @Bean
    @DependsOn("entityManagerFactory")
    public CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {

            if (roleRepository.findByName(ERole.ROLE_USER).isEmpty()) {
                Role userRole = new Role(); // 1. Nesneyi oluştur
                userRole.setName(ERole.ROLE_USER); // 2. Değerini ata
                roleRepository.save(userRole); // 3. Kaydet
            }
            if (roleRepository.findByName(ERole.ROLE_CLUB_OWNER).isEmpty()) {
                Role clubOwnerRole = new Role();
                clubOwnerRole.setName(ERole.ROLE_CLUB_OWNER);
                roleRepository.save(clubOwnerRole);
            }
            if (roleRepository.findByName(ERole.ROLE_ADMIN).isEmpty()) {
                Role adminRole = new Role();
                adminRole.setName(ERole.ROLE_ADMIN);
                roleRepository.save(adminRole);
            }
        };
    }
}