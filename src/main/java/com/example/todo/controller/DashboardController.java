package com.example.todo.controller;

import com.example.todo.config.CurrentUserHelper;
import com.example.todo.domain.AppUser;
import com.example.todo.domain.Todo;
import com.example.todo.domain.Workspace;
import com.example.todo.service.TodoService;
import com.example.todo.service.WorkspaceService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final TodoService TodoService;
    private final WorkspaceService workspaceService;
    private final CurrentUserHelper userHelper;

    public DashboardController(TodoService TodoService,
                               WorkspaceService workspaceService,
                               CurrentUserHelper userHelper) {
        this.TodoService = TodoService;
        this.workspaceService = workspaceService;
        this.userHelper = userHelper;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {

        boolean loggedIn = auth != null && auth.getPrincipal() instanceof OAuth2User;
        model.addAttribute("loggedIn", loggedIn);

        if (!loggedIn) {
            model.addAttribute("todayTasks", List.of());
            model.addAttribute("weekTasks", List.of());
            model.addAttribute("overdueTasks", List.of());
            model.addAttribute("urgentTasks", List.of());
            return "dashboard";
        }

        AppUser user = userHelper.getCurrentUser(auth);
        List<Workspace> workspaces = workspaceService.getMyWorkspaces(user.getId());

        List<Todo> todos = List.of();
        if (!workspaces.isEmpty()) {
            todos = TodoService.findAll(workspaces.get(0).getId(), user.getId());
        }

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

        model.addAttribute("todayTasks", todos.stream()
                .filter(t -> !t.isCompleted() && t.getDeadline() != null)
                .filter(t -> t.getDeadline().isEqual(today))
                .collect(Collectors.toList()));

        model.addAttribute("weekTasks", todos.stream()
                .filter(t -> !t.isCompleted() && t.getDeadline() != null)
                .filter(t -> !t.getDeadline().isBefore(startOfWeek) && !t.getDeadline().isAfter(endOfWeek))
                .collect(Collectors.toList()));

        model.addAttribute("overdueTasks", todos.stream()
                .filter(t -> !t.isCompleted() && t.getDeadline() != null)
                .filter(t -> t.getDeadline().isBefore(today))
                .collect(Collectors.toList()));

        model.addAttribute("urgentTasks", todos.stream()
                .filter(t -> !t.isCompleted() && t.getDeadline() != null)
                .sorted((a, b) -> a.getDeadline().compareTo(b.getDeadline()))
                .limit(5)
                .collect(Collectors.toList()));

        model.addAttribute("workspaces", workspaces);
        return "dashboard";
    }
}