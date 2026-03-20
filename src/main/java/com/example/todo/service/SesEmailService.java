package com.example.todo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class SesEmailService {

    private final SesClient sesClient;
    private final String fromEmail;

    public SesEmailService(
            @Value("${aws.ses.region}") String region,
            @Value("${aws.ses.from-email}") String fromEmail) {

        this.sesClient = SesClient.builder()
                .region(Region.of(region))
                .build();
        this.fromEmail = fromEmail;
    }

    public void sendEmail(String toEmail, String subject, String htmlBody) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder()
                            .toAddresses(toEmail)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                    .build())
                            .build())
                    .build();

            sesClient.sendEmail(request);
        } catch (SesException e) {
            System.err.println("SES 이메일 발송 실패: " + e.getMessage());
        }
    }

    /** 일정 알림 이메일 */
    public void sendTodoReminder(String toEmail, String todoTitle,
                                 String deadline, String reminderType) {

        String typeLabel = switch (reminderType) {
            case "ON_DAY" -> "오늘";
            case "DAY_BEFORE" -> "내일";
            case "WEEK_BEFORE" -> "다음 주";
            default -> "";
        };

        String subject = String.format("[TaskAll] %s 마감 일정 알림: %s", typeLabel, todoTitle);

        String html = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: linear-gradient(135deg, #4facfe, #00c6ff); padding: 30px; border-radius: 12px 12px 0 0;">
                    <h1 style="color: white; margin: 0;">📋 TaskAll</h1>
                </div>
                <div style="background: white; padding: 30px; border: 1px solid #e0e0e0; border-radius: 0 0 12px 12px;">
                    <h2 style="color: #333;">일정 알림</h2>
                    <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <p style="margin: 5px 0;"><strong>일정:</strong> %s</p>
                        <p style="margin: 5px 0;"><strong>마감일:</strong> %s</p>
                        <p style="margin: 5px 0;"><strong>알림:</strong> %s 마감 예정</p>
                    </div>
                    <a href="https://taskall.click/dashboard"
                       style="display: inline-block; background: #4facfe; color: white; padding: 12px 24px; border-radius: 8px; text-decoration: none;">
                        일정 확인하기
                    </a>
                </div>
            </div>
            """, todoTitle, deadline, typeLabel);

        sendEmail(toEmail, subject, html);
    }

    /** 워크스페이스 초대 이메일 */
    public void sendInvitationEmail(String toEmail, String workspaceName,
                                    String inviterName, String token, String baseUrl) {

        String subject = String.format("[TaskAll] %s님이 '%s' 워크스페이스에 초대했습니다",
                inviterName, workspaceName);

        String acceptUrl = baseUrl + "/workspace/invite/accept?token=" + token;

        String html = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: linear-gradient(135deg, #4facfe, #00c6ff); padding: 30px; border-radius: 12px 12px 0 0;">
                    <h1 style="color: white; margin: 0;">📋 TaskAll</h1>
                </div>
                <div style="background: white; padding: 30px; border: 1px solid #e0e0e0; border-radius: 0 0 12px 12px;">
                    <h2 style="color: #333;">워크스페이스 초대</h2>
                    <p>%s님이 <strong>%s</strong> 워크스페이스에 초대했습니다.</p>
                    <p>아래 버튼을 클릭하여 초대를 수락하세요.</p>
                    <a href="%s"
                       style="display: inline-block; background: #4facfe; color: white; padding: 12px 24px; border-radius: 8px; text-decoration: none; margin-top: 15px;">
                        초대 수락하기
                    </a>
                    <p style="color: #999; font-size: 12px; margin-top: 20px;">
                        이 초대는 7일간 유효합니다.
                    </p>
                </div>
            </div>
            """, inviterName, workspaceName, acceptUrl);

        sendEmail(toEmail, subject, html);
    }
}