package com.example.todo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/dashboard", "/calendar", "/todo",
                                "/css/**", "/js/**", "/api/calendar/**","/h2-console/**",

                                // 🔥 추가 (중요)
                                "/todoNew",
                                "/todo/add",
                                "/todo/delete/**",
                                "/todo/update/**",
                                "/todo/toggle/**",
                                "/todo/reorder",
                                "/todo/deleteRepeat"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl("/dashboard", true)
                )

                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                );

        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}