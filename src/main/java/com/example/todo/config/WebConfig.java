package com.example.todo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * ALB(HTTPS) → ECS(HTTP) 환경에서 OAuth2 콜백 처리를 위한 설정.
 *
 * 문제 원인:
 * 1. 사용자가 https://taskall.click 으로 접속
 * 2. ALB가 HTTP로 ECS에 전달 (X-Forwarded-Proto: https, X-Forwarded-Host: taskall.click)
 * 3. Spring이 자신을 http://내부IP:8080 으로 인식
 * 4. OAuth2 authorization request의 redirect_uri가 내부 주소로 생성됨
 * 5. Cognito에서 돌아올 때 redirect_uri 불일치 → authorization_request_not_found
 *
 * 해결:
 * ForwardedHeaderFilter가 X-Forwarded-* 헤더를 처리하여
 * Spring이 자신을 https://taskall.click 으로 올바르게 인식하게 함.
 */
@Configuration
public class WebConfig {

    @Bean
    ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
}