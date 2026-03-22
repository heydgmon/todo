package com.example.todo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        String redirectUrl = request.getParameter("redirectUrl");
        if (redirectUrl != null && !redirectUrl.isBlank()) {
            request.getSession().setAttribute("redirectUrl", redirectUrl);
        }
        return "redirect:/oauth2/authorization/cognito";
    }
}