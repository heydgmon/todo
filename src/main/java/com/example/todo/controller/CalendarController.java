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
import java.util.*;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final TodoService TodoService;
    private final WorkspaceService workspaceService;
    private final CurrentUserHelper userHelper;

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

            // ===== [수정] 시간 정보가 있으면 ISO datetime, 없으면 date만 =====
            if (todo.getStartTime() != null) {
                // "2025-03-21T09:00:00" 형태 → FullCalendar가 시간 블록으로 표시
                event.put("start", todo.getDeadline().atTime(todo.getStartTime()).toString());
                if (todo.getEndTime() != null) {
                    event.put("end", todo.getDeadline().atTime(todo.getEndTime()).toString());
                }
            } else {
                // 기존: 종일 이벤트
                event.put("start", todo.getDeadline().toString());
                event.put("allDay", true);
            }

            event.put("description", todo.getDescription());
            event.put("location", todo.getLocation());
            event.put("completed", todo.isCompleted());

            // ===== [추가] 프론트에서 시간 정보 접근용 =====
            if (todo.getStartTime() != null) {
                event.put("startTime", todo.getStartTime().toString());
            }
            if (todo.getEndTime() != null) {
                event.put("endTime", todo.getEndTime().toString());
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

        // 드래그 시 날짜+시간 모두 처리
        String dateStr = data.get("date");
        if (dateStr != null && dateStr.contains("T")) {
            // "2025-03-21T10:00:00" 형태
            var ldt = java.time.LocalDateTime.parse(dateStr);
            todo.setDeadline(ldt.toLocalDate());
            todo.setStartTime(ldt.toLocalTime());
        } else if (dateStr != null) {
            todo.setDeadline(LocalDate.parse(dateStr));
        }

        TodoService.save(todo);
    }
}