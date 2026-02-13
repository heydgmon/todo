package com.example.todo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SettingsController {

    @GetMapping("/settings")
    public String settings(Authentication authentication, Model model) {

        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User user) {

            model.addAttribute("name", user.getAttribute("name"));
            model.addAttribute("email", user.getAttribute("email"));
            model.addAttribute("attributes", user.getAttributes());
        }

        return "settings";
    }
}
