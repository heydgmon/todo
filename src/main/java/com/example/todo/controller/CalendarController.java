package com.example.todo.controller;

import com.example.todo.config.CurrentUserHelper;
import com.example.todo.domain.AppUser;
import com.example.todo.domain.Todo;
import com.example.todo.domain.Workspace;
import com.example.todo.service.TodoService;
import com.example.todo.service.WorkspaceService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final TodoService TodoService;
    private final WorkspaceService workspaceService;
    private final CurrentUserHelper userHelper;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public CalendarController(TodoService TodoService,
                              WorkspaceService workspaceService,
                              CurrentUserHelper userHelper) {
        this.TodoService = TodoService;
        this.workspaceService = workspaceService;
        this.userHelper = userHelper;
    }

    private boolean isLoggedIn(Authentication auth) {
        return auth != null && auth.getPrincipal() instanceof OAuth2User;
    }

    private Long getDefaultWorkspaceId(AppUser user) {
        List<Workspace> workspaces = workspaceService.getMyWorkspaces(user.getId());
        return workspaces.isEmpty() ? null : workspaces.get(0).getId();
    }

    @GetMapping("/todo")
    public List<Map<String, Object>> getTodosForCalendar(Authentication auth) {

        if (!isLoggedIn(auth)) return List.of();

        AppUser user = userHelper.getCurrentUser(auth);
        Long wsId = getDefaultWorkspaceId(user);
        if (wsId == null) return List.of();

        List<Todo> todos = TodoService.findAll(wsId, user.getId());
        List<Map<String, Object>> events = new ArrayList<>();

        for (Todo todo : todos) {
            if (todo.getDeadline() == null) continue;

            Map<String, Object> event = new HashMap<>();

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

            // ★★★ 핵심: startTime/endTime은 FullCalendar의 recurring event 예약어 ★★★
            // 최상위에 두면 "매일 반복 이벤트"로 해석됨!
            // → "sTime"/"eTime"으로 이름을 바꿔서 extendedProps에 들어가게 함
            event.put("description", todo.getDescription());
            event.put("location", todo.getLocation());
            event.put("completed", todo.isCompleted());

            if (todo.getStartTime() != null) {
                String dateStr = todo.getDeadline().toString();
                String startStr = todo.getStartTime().format(TIME_FMT);

                event.put("start", dateStr + "T" + startStr);
                event.put("allDay", false);

                if (todo.getEndTime() != null) {
                    event.put("end", dateStr + "T" + todo.getEndTime().format(TIME_FMT));
                } else {
                    event.put("end", dateStr + "T" + todo.getStartTime().plusHours(1).format(TIME_FMT));
                }

                // ★ "sTime"/"eTime"으로 — FullCalendar 예약어 회피
                event.put("sTime", startStr);
                if (todo.getEndTime() != null) {
                    event.put("eTime", todo.getEndTime().format(TIME_FMT));
                }
            } else {
                event.put("start", todo.getDeadline().toString());
                event.put("allDay", true);
            }

            events.add(event);
        }

        return events;
    }

    @PostMapping("/updateDate")
    public void updateDate(@RequestBody Map<String, String> data,
                           Authentication auth) {

        if (!isLoggedIn(auth)) return;

        AppUser user = userHelper.getCurrentUser(auth);
        Long id = Long.parseLong(data.get("id"));

        Todo todo = TodoService.findById(id);
        workspaceService.checkPermission(
                todo.getWorkspace().getId(), user.getId(), "EDITOR");

        String dateStr = data.get("date");
        if (dateStr != null && dateStr.contains("T")) {
            var ldt = java.time.LocalDateTime.parse(dateStr);
            todo.setDeadline(ldt.toLocalDate());
            todo.setStartTime(ldt.toLocalTime());
        } else if (dateStr != null) {
            todo.setDeadline(LocalDate.parse(dateStr));
        }

        TodoService.save(todo);
    }
}