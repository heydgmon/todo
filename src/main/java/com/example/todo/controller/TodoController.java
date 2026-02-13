package com.example.todo.controller;

import com.example.todo.domain.Todo;
import com.example.todo.repository.TodoRepository;
import com.example.todo.service.Todoservice;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
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

    @GetMapping("/todo")
    public String list(Model model) {
        model.addAttribute("todos", todoService.findAll());
        return "todo";
    }

    @PostMapping("/todo/add")
    public String add(
            @RequestParam String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate deadline
    ) {
        if (deadline == null) {
            deadline = LocalDate.now();
        }
        todoService.add(title, deadline);
        return "redirect:/todo";
    }

    @PostMapping("/todo/delete/{id}")
    public String delete(@PathVariable Long id) {
        todoService.delete(id);
        return "redirect:/todo";
    }

    @PostMapping("/todo/toggle/{id}")
    public String toggle(@PathVariable Long id) {
        todoService.toggle(id);
        return "redirect:/todo";
    }



}
