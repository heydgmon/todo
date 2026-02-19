package com.example.todo.controller;

import com.example.todo.domain.Todo;
import com.example.todo.service.Todoservice;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final Todoservice todoService;

    public DashboardController(Todoservice todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        List<Todo> todos = todoService.findAll();

        LocalDate today = LocalDate.now();

        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

        // 📌 오늘 할 일
        List<Todo> todayTasks = todos.stream()
                .filter(t -> !t.isCompleted())
                .filter(t -> t.getDeadline() != null)
                .filter(t -> t.getDeadline().isEqual(today))
                .collect(Collectors.toList());

        // 📅 이번 주 일정
        List<Todo> weekTasks = todos.stream()
                .filter(t -> !t.isCompleted())
                .filter(t -> t.getDeadline() != null)
                .filter(t -> !t.getDeadline().isBefore(startOfWeek)
                        && !t.getDeadline().isAfter(endOfWeek))
                .collect(Collectors.toList());

        // ⏳ 지연된 일정
        List<Todo> overdueTasks = todos.stream()
                .filter(t -> !t.isCompleted())
                .filter(t -> t.getDeadline() != null)
                .filter(t -> t.getDeadline().isBefore(today))
                .collect(Collectors.toList());

        // 🔥 가장 임박한 일정 TOP5
        List<Todo> urgentTasks = todos.stream()
                .filter(t -> !t.isCompleted())
                .filter(t -> t.getDeadline() != null)
                .sorted((a, b) -> a.getDeadline().compareTo(b.getDeadline()))
                .limit(5)
                .collect(Collectors.toList());

        model.addAttribute("todayTasks", todayTasks);
        model.addAttribute("weekTasks", weekTasks);
        model.addAttribute("overdueTasks", overdueTasks);
        model.addAttribute("urgentTasks", urgentTasks);

        return "dashboard";
    }
}
