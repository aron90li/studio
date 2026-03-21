package com.aron.studio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public static void main(String[] args) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        // System.out.println(passwordEncoder.encode("admin"));

        boolean match = passwordEncoder.matches("admin",
                "$2a$10$n7GshUuBhFhfoRk9u.GC/uXzSqu7M6DsoQtOkx0iBKFO7eZc6GiKq");
        System.out.println("match: " +  match);
    }
}


