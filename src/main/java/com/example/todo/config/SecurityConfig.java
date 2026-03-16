package com.example.todo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/",
                        "/dashboard",
                        "/calendar",
                        "/todo",
                        "/css/**",
                        "/js/**",
                        "/api/calendar/todo"
                ).permitAll()
                .requestMatchers(
                        "/todo/add",
                        "/todoNew",
                        "/todo/delete/**",
                        "/todo/update/**",
                        "/todo/toggle/**",
                        "/todo/reorder"
                ).authenticated()
                .anyRequest().permitAll()
        );
        http.oauth2Login(oauth -> oauth
                .defaultSuccessUrl("/dashboard", true)   // 👈 추가
        );
        http.oauth2Login(Customizer.withDefaults());

        http.logout(Customizer.withDefaults());

        return http.build();
    }
}