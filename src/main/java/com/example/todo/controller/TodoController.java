package com.example.todo.controller;

import com.example.todo.service.Todoservice;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
public class TodoController {

    private final Todoservice todoService;

    public TodoController(Todoservice todoService) {
        this.todoService = todoService;
    }

    // ==========================
    // TODO 목록 페이지
    // ==========================
    @GetMapping("/todo")
    public String list(Model model) {
        model.addAttribute("todos", todoService.findAll());
        return "todo";
    }

    // ==========================
    // 기존 TODO 추가 (todo 페이지용)
    // ==========================
    @PostMapping("/todo/add")
    public String add(
            @RequestParam String title,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate deadline
    ) {

        if (deadline == null) {
            deadline = LocalDate.now();
        }

        todoService.add(title, deadline);
        return "redirect:/todo";
    }

    // ==========================
    // 🔥 캘린더에서 추가
    // ==========================
    @PostMapping("/todoNew")
    public String addFromCalendar(
            @RequestParam String title,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueDate
    ) {

        todoService.add(title, dueDate);

        return "redirect:/calendar";
    }

    // ==========================
    // 삭제
    // ==========================
    @PostMapping("/todo/delete/{id}")
    public String delete(@PathVariable Long id) {
        todoService.delete(id);
        return "redirect:/todo";
    }

    // ==========================
    // 완료 토글
    // ==========================
    @PostMapping("/todo/toggle/{id}")
    public String toggle(@PathVariable Long id) {
        todoService.toggle(id);
        return "redirect:/todo";
    }
}
