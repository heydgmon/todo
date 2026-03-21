package com.example.todo.service;
import java.time.LocalDateTime;
import com.example.todo.domain.*;
import com.example.todo.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ReminderScheduler {

    private final TodoRepository todoRepo;
    private final WorkspaceMemberRepository memberRepo;
    private final NotificationSettingRepository notifSettingRepo;
    private final NotificationLogRepository notifLogRepo;
    private final SesEmailService emailService;

    public ReminderScheduler(TodoRepository todoRepo,
                             WorkspaceMemberRepository memberRepo,
                             NotificationSettingRepository notifSettingRepo,
                             NotificationLogRepository notifLogRepo,
                             SesEmailService emailService) {
        this.todoRepo = todoRepo;
        this.memberRepo = memberRepo;
        this.notifSettingRepo = notifSettingRepo;
        this.notifLogRepo = notifLogRepo;
        this.emailService = emailService;
    }
    /** 매일 새벽 3시 - 완료 후 3일 지난 Todo → 아카이브 후 삭제 */
    @Scheduled(cron = "0 0 3 * * *")
    public void archiveAndDeleteOldTodos() {
        LocalDate cutoff = LocalDate.now().minusDays(3);
        todoRepo.archiveCompleted(cutoff);
        todoRepo.deleteCompletedBefore(cutoff);
    }

    /** 매일 새벽 3시 30분 - 7일 지난 알림 로그 삭제 */
    @Scheduled(cron = "0 30 3 * * *")
    public void cleanOldNotificationLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        notifLogRepo.deleteBefore(cutoff);
    }
    /** 매일 오전 9시 실행 */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyReminders() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate nextWeek = today.plusDays(7);

        // 당일 마감
        processReminders(today, "ON_DAY");
        // D-1 마감
        processReminders(tomorrow, "DAY_BEFORE");
        // D-7 마감
        processReminders(nextWeek, "WEEK_BEFORE");
    }

    private void processReminders(LocalDate targetDate, String notificationType) {
        List<Todo> todos = todoRepo.findUncompletedByDeadline(targetDate);

        for (Todo todo : todos) {
            Long workspaceId = todo.getWorkspace().getId();

            // 해당 워크스페이스의 모든 멤버에게 알림
            List<WorkspaceMember> members = memberRepo.findByWorkspaceId(workspaceId);

            for (WorkspaceMember member : members) {
                if (!member.isAccepted()) continue;

                AppUser user = member.getUser();

                // 알림 설정 확인
                NotificationSetting setting = notifSettingRepo
                        .findByUserIdAndWorkspaceId(user.getId(), workspaceId)
                        .orElse(null);

                if (setting != null && !setting.isEmailEnabled()) continue;
                if (setting != null && !isNotificationEnabled(setting, notificationType)) continue;

                // 중복 발송 체크
                if (notifLogRepo.existsByTodoIdAndUserIdAndNotificationType(
                        todo.getId(), user.getId(), notificationType)) {
                    continue;
                }

                // 이메일 발송
                emailService.sendTodoReminder(
                        user.getEmail(),
                        todo.getTitle(),
                        todo.getDeadline().toString(),
                        notificationType
                );

                // 발송 이력 기록
                NotificationLog log = new NotificationLog();
                log.setTodo(todo);
                log.setUser(user);
                log.setNotificationType(notificationType);
                notifLogRepo.save(log);
            }
        }
    }

    private boolean isNotificationEnabled(NotificationSetting s, String type) {
        return switch (type) {
            case "ON_DAY" -> s.isNotifyOnDay();
            case "DAY_BEFORE" -> s.isNotifyDayBefore();
            case "WEEK_BEFORE" -> s.isNotifyWeekBefore();
            default -> false;
        };
    }
}