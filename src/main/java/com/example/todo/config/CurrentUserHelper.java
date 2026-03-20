package com.example.todo.config;

import com.example.todo.domain.AppUser;
import com.example.todo.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserHelper {

    private final UserService userService;

    public CurrentUserHelper(UserService userService) {
        this.userService = userService;
    }

    public AppUser getCurrentUser(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof OAuth2User oAuth2User)) {
            return null;
        }
        // findBySubject 대신 getOrCreateUser → 없으면 자동 생성
        return userService.getOrCreateUser(oAuth2User);
    }
}