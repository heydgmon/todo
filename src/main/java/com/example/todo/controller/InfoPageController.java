package com.example.todo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 정적 정보 페이지 컨트롤러
 * - 소개 (About)
 * - 개인정보 처리방침 (Privacy Policy)
 * - 문의 (Contact)
 */
@Controller
public class InfoPageController {

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }
}