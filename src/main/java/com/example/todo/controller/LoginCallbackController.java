package com.example.todo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginCallbackController {

    @GetMapping("/login/callback")
    public String callback() {
        // TODO:
        // 1. code → token 교환 (JS or 백엔드)
        // 2. access token 저장 (쿠키 권장)
        return "redirect:/dashboard";
    }
}