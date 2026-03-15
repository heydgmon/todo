package com.example.todo.service;

import com.example.todo.domain.Todo;
import com.example.todo.repository.TodoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class Todoservice {

    private final TodoRepository repo;

    public Todoservice(TodoRepository repo) {
        this.repo = repo;
    }

    public List<Todo> findAll() {
        return repo.findAll(
                Sort.by("sortOrder").ascending()
        );
    }
    public void reorder(List<Long> ids) {

        int order = 0;

        for(Long id : ids){

            Todo todo = repo.findById(id).orElseThrow();

            todo.setSortOrder(order++);

        }

    }
    public List<Todo> findByProject(String project){
        return repo.findByProject(project);
    }
    public List<Todo> todayTasks(){

        LocalDate today = LocalDate.now();

        return repo.findAll().stream()
                .filter(t -> t.getDeadline()!=null)
                .filter(t -> t.getDeadline().isEqual(today))
                .toList();

    }
    public List<Todo> weekTasks(){

        LocalDate today = LocalDate.now();

        LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(java.time.DayOfWeek.SUNDAY);

        return repo.findAll().stream()
                .filter(t -> t.getDeadline() != null)
                .filter(t -> !t.getDeadline().isBefore(startOfWeek))
                .filter(t -> !t.getDeadline().isAfter(endOfWeek))
                .toList();

    }
    public Todo findById(Long id) {
        return repo.findById(id).orElseThrow();
    }

    public void save(Todo todo) {
        repo.save(todo);
    }

    // 기본 추가 (프로젝트/태그/서브태스크 포함)
    public void add(
            String title,
            String description,
            LocalDate deadline,
            String priority,
            String color,
            String repeatType,
            String project,
            Long parentId,
            String tagsCsv
    ) {

        Todo todo = new Todo();

        todo.setTitle(title);
        todo.setDescription(description);
        todo.setDeadline(deadline);
        todo.setPriority(priority);
        todo.setColor(color);
        todo.setRepeatType(repeatType);
        todo.setProject(project);
        todo.setCompleted(false);

        if (parentId != null) {
            todo.setParent(findById(parentId));
        }

        if (tagsCsv != null && !tagsCsv.isBlank()) {
            Set<String> tags = new HashSet<>(Arrays.asList(tagsCsv.split(",")));
            todo.setTags(tags);
        }

        repo.save(todo);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public void toggle(Long id) {
        Todo todo = findById(id);
        todo.setCompleted(!todo.isCompleted());
    }

    // 검색
    public List<Todo> search(String q) {
        return repo.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrProjectContainingIgnoreCase(
                q, q, q
        );
    }
}