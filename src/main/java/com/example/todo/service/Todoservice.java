package com.example.todo.service;

import com.example.todo.domain.Todo;
import com.example.todo.repository.TodoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.time.LocalDate;

@Service
@Transactional
public class Todoservice {

    private final TodoRepository repo;

    public Todoservice(TodoRepository repo) {
        this.repo = repo;
    }

    public List<Todo> findAll() {
        return repo.findAll(
                Sort.by("completed").and(Sort.by("id"))
        );
    }
    public void add(String title, LocalDate deadline) {
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setCompleted(false);
        todo.setDeadline(deadline);
        repo.save(todo);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public void toggle(Long id) {
        Todo todo = repo.findById(id).orElseThrow();
        todo.setCompleted(!todo.isCompleted());
    }


}
