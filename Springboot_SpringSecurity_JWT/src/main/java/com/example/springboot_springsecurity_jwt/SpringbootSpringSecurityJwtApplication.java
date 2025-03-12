package com.example.springboot_springsecurity_jwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SpringbootSpringSecurityJwtApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootSpringSecurityJwtApplication.class, args);
    }

}
