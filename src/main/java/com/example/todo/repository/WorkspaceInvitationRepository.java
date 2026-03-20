package com.example.todo.repository;

import com.example.todo.domain.WorkspaceInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {
    Optional<WorkspaceInvitation> findByToken(String token);
}