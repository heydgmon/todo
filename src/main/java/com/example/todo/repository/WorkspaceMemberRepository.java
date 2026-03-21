package com.example.todo.repository;

import com.example.todo.domain.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    List<WorkspaceMember> findByWorkspaceId(Long workspaceId);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);
    void deleteByWorkspaceId(Long workspaceId);
    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);
}