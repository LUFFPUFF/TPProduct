package com.example.ui.run;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@SpringBootApplication
@ComponentScan(basePackages = "com.example")
@EntityScan(basePackages = "com.example.database.model")
@EnableJpaRepositories(basePackages = "com.example.database.repository")
@EnableTransactionManagement
@EnableCaching
public class SpringBootStartApplication {




    public static void main(String[] args) {
       SpringApplication.run(SpringBootStartApplication.class, args);


    }
}
