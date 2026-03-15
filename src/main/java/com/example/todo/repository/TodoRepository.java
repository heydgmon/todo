package com.example.todo.repository;

import com.example.todo.domain.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // 4) 검색 (title/description/project)
    List<Todo> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrProjectContainingIgnoreCase(
            String title, String description, String project
    );
}