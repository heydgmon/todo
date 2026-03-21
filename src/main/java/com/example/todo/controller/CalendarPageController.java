package com.example.todo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CalendarPageController {

    @GetMapping("/calendar")
    public String calendar(Authentication auth, Model model) {
        boolean loggedIn = auth != null && auth.getPrincipal() instanceof OAuth2User;
        model.addAttribute("loggedIn", loggedIn);
        return "calendar";
    }
}