package com.example.todo.service;

import com.example.todo.domain.AppUser;
import com.example.todo.domain.Todo;
import com.example.todo.domain.Workspace;
import com.example.todo.repository.TodoRepository;
import com.example.todo.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Transactional
public class TodoService {

    private final TodoRepository repo;
    private final WorkspaceRepository workspaceRepo;
    private final WorkspaceService workspaceService;

    public TodoService(TodoRepository repo,
                       WorkspaceRepository workspaceRepo,
                       WorkspaceService workspaceService) {
        this.repo = repo;
        this.workspaceRepo = workspaceRepo;
        this.workspaceService = workspaceService;
    }

    public List<Todo> findAll(Long workspaceId, Long userId) {
        workspaceService.checkPermission(workspaceId, userId, "VIEWER");
        return repo.findByWorkspaceIdOrderBySortOrderAsc(workspaceId);
    }

    public void reorder(Long workspaceId, Long userId, List<Long> ids) {
        workspaceService.checkPermission(workspaceId, userId, "EDITOR");
        int order = 0;
        for (Long id : ids) {
            Todo todo = repo.findById(id).orElseThrow();
            todo.setSortOrder(order++);
        }
    }

    public void deleteRepeat(Long workspaceId, Long userId, String title, String repeatType) {
        workspaceService.checkPermission(workspaceId, userId, "EDITOR");
        repo.deleteByWorkspaceIdAndTitleAndRepeatType(workspaceId, title, repeatType);
    }

    public List<Todo> findByProject(Long workspaceId, Long userId, String project) {
        workspaceService.checkPermission(workspaceId, userId, "VIEWER");
        return repo.findByWorkspaceIdAndProject(workspaceId, project);
    }

    public List<Todo> todayTasks(Long workspaceId, Long userId) {
        workspaceService.checkPermission(workspaceId, userId, "VIEWER");
        LocalDate today = LocalDate.now();
        return repo.findByWorkspaceIdOrderBySortOrderAsc(workspaceId).stream()
                .filter(t -> t.getDeadline() != null && t.getDeadline().isEqual(today))
                .toList();
    }

    public List<Todo> weekTasks(Long workspaceId, Long userId) {
        workspaceService.checkPermission(workspaceId, userId, "VIEWER");
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);
        return repo.findByWorkspaceIdOrderBySortOrderAsc(workspaceId).stream()
                .filter(t -> t.getDeadline() != null)
                .filter(t -> !t.getDeadline().isBefore(startOfWeek))
                .filter(t -> !t.getDeadline().isAfter(endOfWeek))
                .toList();
    }

    public Todo findById(Long id) {
        return repo.findById(id).orElseThrow();
    }

    public void save(Todo todo) {
        todo.setUpdatedAt(LocalDateTime.now());
        repo.save(todo);
    }

    // 기존 add() — /todo/add 에서 호출 (시간/장소 없는 경우)
    public void add(Long workspaceId, AppUser user,
                    String title, String description, LocalDate deadline,
                    String priority, String color, String repeatType,
                    String project, Long parentId, String tagsCsv) {

        addWithLocation(workspaceId, user, title, description, deadline,
                priority, color, repeatType, project, parentId, tagsCsv,
                null, null, null, null, null);
    }

    // addWithLocation() — /todoNew 에서 호출 (시간+장소 포함)
    public void addWithLocation(Long workspaceId, AppUser user,
                                String title, String description, LocalDate deadline,
                                String priority, String color, String repeatType,
                                String project, Long parentId, String tagsCsv,
                                String location, Double lat, Double lng,
                                LocalTime startTime, LocalTime endTime) {

        workspaceService.checkPermission(workspaceId, user.getId(), "EDITOR");
        Workspace ws = workspaceRepo.findById(workspaceId).orElseThrow();

        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setDescription(description);
        todo.setDeadline(deadline);
        todo.setPriority(priority);
        todo.setColor(color);
        todo.setRepeatType(repeatType);
        todo.setProject(project);
        todo.setCompleted(false);
        todo.setWorkspace(ws);
        todo.setCreatedBy(user);

        // 장소 정보
        if (location != null && !location.isBlank()) {
            todo.setLocation(location);
        }
        if (lat != null) todo.setLat(lat);
        if (lng != null) todo.setLng(lng);

        // ===== [추가] 시간 정보 =====
        if (startTime != null) todo.setStartTime(startTime);
        if (endTime != null) todo.setEndTime(endTime);

        if (deadline != null) {
            todo.setReminderTime(deadline.atStartOfDay().minusMinutes(10));
        }
        todo.setNotified(false);

        if (parentId != null) {
            todo.setParent(findById(parentId));
        }

        if (tagsCsv != null && !tagsCsv.isBlank()) {
            Set<String> tags = new HashSet<>(Arrays.asList(tagsCsv.split(",")));
            todo.setTags(tags);
        }

        repo.save(todo);
    }

    public void delete(Long id, Long workspaceId, Long userId) {
        workspaceService.checkPermission(workspaceId, userId, "EDITOR");
        repo.deleteById(id);
    }

    public void toggle(Long id, Long workspaceId, Long userId) {
        workspaceService.checkPermission(workspaceId, userId, "EDITOR");
        Todo todo = findById(id);
        todo.setCompleted(!todo.isCompleted());
    }

    public List<Todo> search(Long workspaceId, Long userId, String q) {
        workspaceService.checkPermission(workspaceId, userId, "VIEWER");
        return repo.searchInWorkspace(workspaceId, q);
    }
}