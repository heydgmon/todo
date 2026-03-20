package com.example.todo.repository;

import com.example.todo.domain.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    boolean existsByTodoIdAndUserIdAndNotificationType(Long todoId, Long userId, String type);
}