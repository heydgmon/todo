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
                .csrf(csrf -> csrf.disable())   // POST/DELETE 403 방지

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/dashboard",
                                "/calendar",
                                "/todo",
                                "/css/**",
                                "/js/**",
                                "/h2-console/**"
                        ).permitAll()
                        .anyRequest().authenticated()   // 로그인만 하면 모든 기능 사용
                )

                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl("/dashboard", true)
                )

                .logout(Customizer.withDefaults());

        // H2 콘솔 사용 시 필요
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}