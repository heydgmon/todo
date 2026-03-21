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

    // HH:mm 고정 포맷 (나노초/초 방지)
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
            event.put("description", todo.getDescription());
            event.put("location", todo.getLocation());
            event.put("completed", todo.isCompleted());

            // ===== 핵심: 시간 유무에 따라 종일/시간 이벤트 분기 =====
            if (todo.getStartTime() != null) {
                // 시간 이벤트: "2025-03-21T09:00" (초 없이 깔끔하게)
                String dateStr = todo.getDeadline().toString();
                String startStr = todo.getStartTime().format(TIME_FMT);

                event.put("start", dateStr + "T" + startStr);
                event.put("allDay", false);  // ★ 반드시 false 명시

                // ★ end를 반드시 설정 — 없으면 FullCalendar가 이상하게 렌더링
                if (todo.getEndTime() != null) {
                    event.put("end", dateStr + "T" + todo.getEndTime().format(TIME_FMT));
                } else {
                    // end 없으면 start + 1시간으로 기본값
                    event.put("end", dateStr + "T" + todo.getStartTime().plusHours(1).format(TIME_FMT));
                }

                // 상세 모달용
                event.put("startTime", startStr);
                if (todo.getEndTime() != null) {
                    event.put("endTime", todo.getEndTime().format(TIME_FMT));
                }
            } else {
                // 종일 이벤트
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