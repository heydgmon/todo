package com.example.todo.repository;

import com.example.todo.domain.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByUserIdAndWorkspaceId(Long userId, Long workspaceId);
    void deleteByWorkspaceId(Long workspaceId);
}