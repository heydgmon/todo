package com.example.todo.repository;

import com.example.todo.domain.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    boolean existsByTodoIdAndUserIdAndNotificationType(Long todoId, Long userId, String type);
    @Modifying
    @Query("DELETE FROM NotificationLog n WHERE n.sentAt < :cutoff")
    void deleteBefore(@Param("cutoff") LocalDateTime cutoff);
}