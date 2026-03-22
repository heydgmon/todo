package com.example.todo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 정적 정보 페이지 컨트롤러
 * - 소개 (About)
 * - 개인정보 처리방침 (Privacy Policy)
 * - 문의 (Contact)
 * - 사용 가이드 허브 + 개별 가이드 페이지
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

    @GetMapping("/guide")
    public String guide() {
        return "guide";
    }

    @GetMapping("/guide/getting-started")
    public String guideGettingStarted() {
        return "guide/getting-started";
    }

    @GetMapping("/guide/guest-vs-login")
    public String guideGuestVsLogin() {
        return "guide/guest-vs-login";
    }

    @GetMapping("/guide/todo")
    public String guideTodo() {
        return "guide/todo";
    }

    @GetMapping("/guide/calendar")
    public String guideCalendar() {
        return "guide/calendar";
    }

    @GetMapping("/guide/dashboard")
    public String guideDashboard() {
        return "guide/dashboard";
    }

    @GetMapping("/guide/workspace")
    public String guideWorkspace() {
        return "guide/workspace";
    }

    @GetMapping("/guide/notifications")
    public String guideNotifications() {
        return "guide/notifications";
    }

    @GetMapping("/guide/tips")
    public String guideTips() {
        return "guide/tips";
    }
}