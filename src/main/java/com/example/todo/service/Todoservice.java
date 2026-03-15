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
    public void add(
            String title,
            String description,
            LocalDate deadline,
            String priority,
            String color,
            String repeatType
    ) {

        Todo todo = new Todo();

        todo.setTitle(title);
        todo.setDescription(description);
        todo.setCompleted(false);
        todo.setDeadline(deadline);
        todo.setPriority(priority);
        todo.setColor(color);
        todo.setRepeatType(repeatType);

        repo.save(todo);
    }
    public Todo findById(Long id) {
        return repo.findById(id).orElseThrow();
    }
    public void save(Todo todo){
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
