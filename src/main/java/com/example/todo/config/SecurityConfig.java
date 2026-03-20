package com.example.todo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )

                .exceptionHandling(e -> e
                        .defaultAuthenticationEntryPointFor(
                                (request, response, authException) -> {
                                    response.sendError(401);
                                },
                                new AntPathRequestMatcher("/api/**")
                        )
                )

                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .successHandler(oAuth2LoginSuccessHandler)
                        // ✅ 명시적으로 세션 기반 authorization request 저장소 설정
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(
                                        new HttpSessionOAuth2AuthorizationRequestRepository()
                                )
                        )
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                );

        http.headers(headers ->
                headers.frameOptions(frame -> frame.disable())
        );

        return http.build();
    }
}