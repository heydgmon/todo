package com.example.todo.controller;

import com.example.todo.domain.Todo;
import com.example.todo.service.Todoservice;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final Todoservice todoService;

    public CalendarController(Todoservice todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/todo")
    public List<Map<String, Object>> getTodosForCalendar() {

        List<Todo> todos = todoService.findAll();
        LocalDate today = LocalDate.now();

        List<Map<String, Object>> events = new ArrayList<>();

        for (Todo todo : todos) {
            if (todo.getDeadline() == null) continue;

            Map<String, Object> event = new HashMap<>();

            // ✅ 상태를 title 앞에 표시
            String statusLabel = todo.isCompleted() ? "[완료]" : "[미완료]";
            event.put("title", statusLabel + " " + todo.getTitle());

            event.put("id", todo.getId());
            event.put("start", todo.getDeadline().toString());

            // 색상 규칙
            if (todo.isCompleted()) {
                event.put("color", "#6c757d"); // 완료: 회색
            } else if (todo.getDeadline().isBefore(today)) {
                event.put("color", "#212529"); // 지연: 검정
            } else if (todo.getDeadline().isEqual(today)) {
                event.put("color", "#dc3545"); // D-day: 빨강
            } else {
                event.put("color", "#0d6efd"); // 일반: 파랑
            }

            events.add(event);
        }

        return events;
    }
}
