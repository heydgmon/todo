package com.example.todo.controller;

import com.example.todo.service.Todoservice;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final Todoservice todoService;

    public DashboardController(Todoservice todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("todos", todoService.findAll());
        return "dashboard";
    }
}
