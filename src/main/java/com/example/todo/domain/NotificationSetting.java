package com.example.todo.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_setting")
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    private boolean notifyDayBefore = true;
    private boolean notifyWeekBefore = true;
    private boolean notifyOnDay = true;
    private boolean emailEnabled = true;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }

    public boolean isNotifyDayBefore() { return notifyDayBefore; }
    public void setNotifyDayBefore(boolean v) { this.notifyDayBefore = v; }

    public boolean isNotifyWeekBefore() { return notifyWeekBefore; }
    public void setNotifyWeekBefore(boolean v) { this.notifyWeekBefore = v; }

    public boolean isNotifyOnDay() { return notifyOnDay; }
    public void setNotifyOnDay(boolean v) { this.notifyOnDay = v; }

    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean v) { this.emailEnabled = v; }
}