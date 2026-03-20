package com.example.todo.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Integer sortOrder;

    private LocalDate remindDate;
    @Column(length = 500)
    private String description;

    private boolean completed;

    private LocalDate deadline;

    private String priority;   // LOW / MEDIUM / HIGH
    private String color;      // hex
    private String repeatType; // NONE / DAILY / WEEKLY / MONTHLY

    // 1) 서브태스크: 부모 Todo
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Todo parent;

    // 2) 프로젝트/리스트
    private String project;

    // 3) 태그(간단: 문자열 세트)
    @ElementCollection
    @CollectionTable(name = "todo_tags", joinColumns = @JoinColumn(name = "todo_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    //aws ses 알림기능 테스트
    private String email;                 // 사용자 이메일
    private LocalDateTime reminderTime;
    private boolean notified;
    private String location;
    public void setReminderTime(LocalDateTime reminderTime) {
        this.reminderTime = reminderTime;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    // getters/setters
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getRepeatType() { return repeatType; }
    public void setRepeatType(String repeatType) { this.repeatType = repeatType; }

    public Todo getParent() { return parent; }
    public void setParent(Todo parent) { this.parent = parent; }

    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDate getRemindDate() {
        return remindDate;
    }

    public void setRemindDate(LocalDate remindDate) {
        this.remindDate = remindDate;
    }
}