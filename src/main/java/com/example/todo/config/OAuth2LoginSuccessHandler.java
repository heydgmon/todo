package com.example.todo.config;

import com.example.todo.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    public OAuth2LoginSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        userService.getOrCreateUser(oAuth2User);

        // 세션에 저장된 원래 요청 URL 확인 (초대 수락 링크 등)
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            requestCache.removeRequest(request, response);
            response.sendRedirect(targetUrl);
            return;
        }

        // 세션에 직접 저장한 redirectUrl 확인 (LoginController에서 저장)
        HttpSession session = request.getSession(false);
        if (session != null) {
            String redirectUrl = (String) session.getAttribute("redirectUrl");
            if (redirectUrl != null) {
                session.removeAttribute("redirectUrl");
                response.sendRedirect(redirectUrl);
                return;
            }
        }

        // 기본 목적지
        response.sendRedirect("/dashboard");
    }
}