package com.example.todo.controller;
import java.util.Arrays;
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
        List<Map<String, Object>> events = new ArrayList<>();

        for (Todo todo : todos) {

            if (todo.getDeadline() == null) continue;

            Map<String, Object> event = new HashMap<>();
            // ⭐ 여기 추가
            if (todo.isCompleted()) {
                event.put("classNames", Arrays.asList("completed-event"));
            }

            String prefix = "";

            if ("HIGH".equals(todo.getPriority())) {
                prefix = "🔥 ";
                event.put("color", "#dc3545");
            } else if ("MEDIUM".equals(todo.getPriority())) {
                prefix = "⚡ ";
                event.put("color", "#ffc107");
            } else {
                prefix = "🌿 ";
                event.put("color", "#198754");
            }

            event.put("id", todo.getId());
            event.put("title", prefix + todo.getTitle());
            event.put("start", todo.getDeadline().toString());
            // 🔥 설명 추가
            event.put("description", todo.getDescription());
            events.add(event);
        }

        return events;
    }

    @PostMapping("/updateDate")
    public void updateDate(@RequestBody Map<String,String> data){

        Long id = Long.parseLong(data.get("id"));
        LocalDate date = LocalDate.parse(data.get("date"));

        Todo todo = todoService.findById(id);
        todo.setDeadline(date);

        todoService.save(todo);
    }
}