package com.example.todo.repository;

import com.example.todo.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    // 내가 소유하거나 멤버인 워크스페이스 목록
    @Query("SELECT DISTINCT w FROM Workspace w " +
            "LEFT JOIN w.members m " +
            "WHERE w.owner.id = :userId OR (m.user.id = :userId AND m.accepted = true)")
    List<Workspace> findAllByUserId(@Param("userId") Long userId);
}