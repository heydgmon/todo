package com.example.todo.repository;

import com.example.todo.domain.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // 워크스페이스 기준 조회
    List<Todo> findByWorkspaceIdOrderBySortOrderAsc(Long workspaceId);

    // 워크스페이스 내 검색
    @Query("SELECT t FROM Todo t WHERE t.workspace.id = :wsId AND " +
            "(LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(t.project) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Todo> searchInWorkspace(@Param("wsId") Long workspaceId, @Param("q") String query);

    // 프로젝트별
    List<Todo> findByWorkspaceIdAndProject(Long workspaceId, String project);

    // 반복 일정 삭제
    void deleteByWorkspaceIdAndTitleAndRepeatType(Long workspaceId, String title, String repeatType);
    void deleteByWorkspaceId(Long workspaceId);
    // 알림용: 특정 날짜에 마감인 미완료 Todo
    @Query("SELECT t FROM Todo t WHERE t.completed = false AND t.deadline = :date")
    List<Todo> findUncompletedByDeadline(@Param("date") LocalDate date);

    // 알림용: 날짜 범위
    @Query("SELECT t FROM Todo t WHERE t.completed = false AND t.deadline BETWEEN :start AND :end")
    List<Todo> findUncompletedByDeadlineBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Modifying
    @Query(value = "INSERT INTO todo_archive SELECT * FROM todo " +
            "WHERE completed = true AND deadline < :cutoff", nativeQuery = true)
    void archiveCompleted(@Param("cutoff") LocalDate cutoff);

    @Modifying
    @Query("DELETE FROM Todo t WHERE t.completed = true AND t.deadline < :cutoff")
    void deleteCompletedBefore(@Param("cutoff") LocalDate cutoff);
}