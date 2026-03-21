package com.example.todo.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private String priority;
    private String color;
    private String repeatType;

    // ===== [추가] 시간 단위 일정 =====
    private LocalTime startTime;
    private LocalTime endTime;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Todo parent;

    private String project;

    @ElementCollection
    @CollectionTable(name = "todo_tags", joinColumns = @JoinColumn(name = "todo_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    private LocalDateTime reminderTime;
    private boolean notified;

    private String location;
    private Double lat;
    private Double lng;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

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
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDate getRemindDate() { return remindDate; }
    public void setRemindDate(LocalDate remindDate) { this.remindDate = remindDate; }
    public void setReminderTime(LocalDateTime reminderTime) { this.reminderTime = reminderTime; }
    public LocalDateTime getReminderTime() { return reminderTime; }
    public void setNotified(boolean notified) { this.notified = notified; }
    public boolean isNotified() { return notified; }

    // ===== [추가] 시간 getter/setter =====
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}