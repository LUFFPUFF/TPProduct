package com.example.testing_repository;

import com.example.model.company_subscription_module.user_roles.role.Role;
import com.example.model.company_subscription_module.user_roles.role.RoleName;
import com.example.repository.company_subscription_module.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.example")
@EnableJpaRepositories("com.example.repository")
@EntityScan(basePackages = "com.example.model")
public class RoleTest {

    public static void main(String[] args) {
        SpringApplication.run(RoleTest.class, args);
    }

    @Bean
    public CommandLineRunner demoData(RoleRepository roleRepository) {
        return args -> {
            Role role = new Role();
            role.setName(RoleName.MANAGER);
            role.setDescription("Описание менджера");

            roleRepository.save(role);
        };
    }
}
