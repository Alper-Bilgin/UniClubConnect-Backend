package com.uniclubconnect.services.authservice;

import com.uniclubconnect.services.authservice.entity.ERole;
import com.uniclubconnect.services.authservice.entity.Role;
import com.uniclubconnect.services.authservice.entity.User;
import com.uniclubconnect.services.authservice.repository.RoleRepository;
import com.uniclubconnect.services.authservice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@SpringBootApplication
@EnableDiscoveryClient
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @Bean
    @DependsOn("entityManagerFactory")
    public CommandLineRunner initData(UserRepository userRepository,
                                      RoleRepository roleRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {

            // Rolleri oluştur veya var olanları al
            Role userRole = roleRepository.findByName(ERole.ROLE_USER).orElseGet(() ->
                    roleRepository.save(new Role() {{
                        setName(ERole.ROLE_USER);
                    }})
            );

            Role clubOwnerRole = roleRepository.findByName(ERole.ROLE_CLUB_OWNER).orElseGet(() ->
                    roleRepository.save(new Role() {{
                        setName(ERole.ROLE_CLUB_OWNER);
                    }})
            );

            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseGet(() ->
                    roleRepository.save(new Role() {{
                        setName(ERole.ROLE_ADMIN);
                    }})
            );

            // Varsayılan Admin Kullanıcısı Oluştur
            String adminEmail = "admin@uniclub.com";
            if (!userRepository.existsByEmail(adminEmail)) {
                User adminUser = new User(
                        adminEmail,
                        passwordEncoder.encode("admin123") // Şifre: admin123
                );

                // Admin'e tüm rolleri ver
                adminUser.setRoles(Set.of(userRole, clubOwnerRole, adminRole));
                userRepository.save(adminUser);

                System.out.println("✅ Varsayılan admin kullanıcısı oluşturuldu: " + adminEmail);
            } else {
                System.out.println("ℹ️ Varsayılan admin kullanıcısı zaten mevcut: " + adminEmail);
            }
        };
    }
}
