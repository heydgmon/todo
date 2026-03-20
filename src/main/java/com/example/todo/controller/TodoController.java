package com.example.todo.controller;

import com.example.todo.config.CurrentUserHelper;
import com.example.todo.domain.AppUser;
import com.example.todo.domain.Todo;
import com.example.todo.domain.Workspace;
import com.example.todo.service.TodoService;
import com.example.todo.service.WorkspaceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
public class TodoController {

    private final TodoService TodoService;
    private final WorkspaceService workspaceService;
    private final CurrentUserHelper userHelper;

    public TodoController(TodoService TodoService,
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
    public String list(@RequestParam(required = false) String q,
                       Authentication auth, Model model) {

        model.addAttribute("loggedIn", isLoggedIn(auth));

        if (!isLoggedIn(auth)) {
            model.addAttribute("todos", List.of());
            return "todo";
        }

        AppUser user = userHelper.getCurrentUser(auth);
        Long wsId = getDefaultWorkspaceId(user);
        if (wsId == null) {
            model.addAttribute("todos", List.of());
            return "todo";
        }

        List<Todo> todos = (q == null || q.isBlank())
                ? TodoService.findAll(wsId, user.getId())
                : TodoService.search(wsId, user.getId(), q);
        model.addAttribute("todos", todos);
        return "todo";
    }

    @GetMapping("/todo/today")
    public String today(Authentication auth, Model model) {
        model.addAttribute("loggedIn", isLoggedIn(auth));
        if (!isLoggedIn(auth)) {
            model.addAttribute("todos", List.of());
            return "todo";
        }
        AppUser user = userHelper.getCurrentUser(auth);
        Long wsId = getDefaultWorkspaceId(user);
        model.addAttribute("todos", wsId != null
                ? TodoService.todayTasks(wsId, user.getId()) : List.of());
        return "todo";
    }

    @GetMapping("/todo/week")
    public String week(Authentication auth, Model model) {
        model.addAttribute("loggedIn", isLoggedIn(auth));
        if (!isLoggedIn(auth)) {
            model.addAttribute("todos", List.of());
            return "todo";
        }
        AppUser user = userHelper.getCurrentUser(auth);
        Long wsId = getDefaultWorkspaceId(user);
        model.addAttribute("todos", wsId != null
                ? TodoService.weekTasks(wsId, user.getId()) : List.of());
        return "todo";
    }

    @GetMapping("/todo/project/{name}")
    public String project(@PathVariable String name,
                          Authentication auth, Model model) {
        model.addAttribute("loggedIn", isLoggedIn(auth));
        if (!isLoggedIn(auth)) {
            model.addAttribute("todos", List.of());
            return "todo";
        }
        AppUser user = userHelper.getCurrentUser(auth);
        Long wsId = getDefaultWorkspaceId(user);
        model.addAttribute("todos", wsId != null
                ? TodoService.findByProject(wsId, user.getId(), name) : List.of());
        return "todo";
    }

    // ========== POST: 로그인 필수 ==========

    @PostMapping("/todo/reorder")
    @ResponseBody
    public void reorder(@RequestBody List<Long> ids, Authentication auth) {
        if (!isLoggedIn(auth)) return;
        AppUser user = userHelper.getCurrentUser(auth);
        Long wsId = getDefaultWorkspaceId(user);
        if (wsId != null) TodoService.reorder(wsId, user.getId(), ids);
    }

    @PostMapping("/todo/add")
    public String add(@RequestParam String title,
                      @RequestParam(required = false) String description,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
                      @RequestParam(required = false) String priority,
                      @RequestParam(required = false) String color,
                      @RequestParam(required = false) String repeatType,
                      @RequestParam(required = false) String project,
                      @RequestParam(required = false) Long parentId,
                      @RequestParam(required = false) String tags,
                      Authentication auth) {

        if (!isLoggedIn(auth)) return "redirect:/todo";

        AppUser user = userHelper.getCurrentUser(auth);
        Long wsId = getDefaultWorkspaceId(user);
        if (wsId == null) return "redirect:/todo";

        if (deadline == null) deadline = LocalDate.now();
        TodoService.add(wsId, user, title, description, deadline,
                priority, color, repeatType, project, parentId, tags);
        return "redirect:/todo";
    }

    @PostMapping("/todo/update/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam(required = false) String description,
                         Authentication auth) {

        if (!isLoggedIn(auth)) return "redirect:/todo";

        AppUser user = userHelper.getCurrentUser(auth);
        Long wsId = getDefaultWorkspaceId(user);
        if (wsId != null) {
            workspaceService.checkPermission(wsId, user.getId(), "EDITOR");
            Todo todo = TodoService.findById(id);
            todo.setTitle(title);
            todo.setDescription(description);
            TodoService.save(todo);
        }
        return "redirect:/todo";
    }

    @PostMapping("/todo/delete/{id}")
    public String delete(@PathVariable Long id, Authentication auth) {
        if (!isLoggedIn(auth)) return "redirect:/todo";
        AppUser user = userHelper.getCurrentUser(auth);
        Long wsId = getDefaultWorkspaceId(user);
        if (wsId != null) TodoService.delete(id, wsId, user.getId());
        return "redirect:/todo";
    }

    @PostMapping("/todo/toggle/{id}")
    public String toggle(@PathVariable Long id, Authentication auth) {
        if (!isLoggedIn(auth)) return "redirect:/todo";
        AppUser user = userHelper.getCurrentUser(auth);
        Long wsId = getDefaultWorkspaceId(user);
        if (wsId != null) TodoService.toggle(id, wsId, user.getId());
        return "redirect:/todo";
    }

    @PostMapping("/todo/deleteRepeat")
    public String deleteRepeat(@RequestParam String title,
                               @RequestParam String repeatType,
                               Authentication auth) {
        if (!isLoggedIn(auth)) return "redirect:/todo";
        AppUser user = userHelper.getCurrentUser(auth);
        Long wsId = getDefaultWorkspaceId(user);
        if (wsId != null) TodoService.deleteRepeat(wsId, user.getId(), title, repeatType);
        return "redirect:/todo";
    }
    @GetMapping("/ws/{wsId}/todo")
    public String listByWorkspace(@PathVariable Long wsId,
                                  @RequestParam(required = false) String q,
                                  Authentication auth, Model model) {

        if (!isLoggedIn(auth)) return "redirect:/oauth2/authorization/cognito";

        AppUser user = userHelper.getCurrentUser(auth);

        List<Todo> todos = (q == null || q.isBlank())
                ? TodoService.findAll(wsId, user.getId())
                : TodoService.search(wsId, user.getId(), q);

        model.addAttribute("todos", todos);
        model.addAttribute("loggedIn", true);
        model.addAttribute("wsId", wsId);
        return "todo";
    }
    @PostMapping("/todoNew")
    public String addFromCalendar(@RequestParam String title,
                                  @RequestParam(required = false) String description,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                                  @RequestParam(required = false) String priority,
                                  @RequestParam(required = false) String color,
                                  @RequestParam(required = false) String repeatType,
                                  @RequestParam(required = false) String project,
                                  @RequestParam(required = false) Long parentId,
                                  @RequestParam(required = false) String tags,
                                  Authentication auth) {

        if (!isLoggedIn(auth)) return "redirect:/calendar";

        AppUser user = userHelper.getCurrentUser(auth);
        Long wsId = getDefaultWorkspaceId(user);
        if (wsId == null) return "redirect:/calendar";

        if (repeatType == null || repeatType.equals("NONE")) {
            TodoService.add(wsId, user, title, description, dueDate,
                    priority, color, "NONE", project, parentId, tags);
        } else if (repeatType.equals("DAILY")) {
            LocalDate end = dueDate.plusYears(1);
            LocalDate current = dueDate;
            while (!current.isAfter(end)) {
                TodoService.add(wsId, user, title, description, current,
                        priority, color, "DAILY", project, parentId, tags);
                current = current.plusDays(1);
            }
        } else if (repeatType.equals("WEEKLY")) {
            LocalDate end = dueDate.plusYears(1);
            LocalDate current = dueDate;
            while (!current.isAfter(end)) {
                TodoService.add(wsId, user, title, description, current,
                        priority, color, "WEEKLY", project, parentId, tags);
                current = current.plusWeeks(1);
            }
        } else if (repeatType.equals("MONTHLY")) {
            LocalDate end = dueDate.plusYears(1);
            LocalDate current = dueDate;
            while (!current.isAfter(end)) {
                TodoService.add(wsId, user, title, description, current,
                        priority, color, "MONTHLY", project, parentId, tags);
                current = current.plusMonths(1);
            }
        }

        return "redirect:/calendar";
    }
}