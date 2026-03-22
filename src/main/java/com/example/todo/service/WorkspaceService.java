package com.example.todo.service;

import com.example.todo.domain.*;
import com.example.todo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepo;
    private final WorkspaceMemberRepository memberRepo;
    private final WorkspaceInvitationRepository invitationRepo;
    private final AppUserRepository userRepo;
    private final NotificationSettingRepository notifSettingRepo;

    public WorkspaceService(WorkspaceRepository workspaceRepo,
                            WorkspaceMemberRepository memberRepo,
                            WorkspaceInvitationRepository invitationRepo,
                            AppUserRepository userRepo,
                            NotificationSettingRepository notifSettingRepo) {
        this.workspaceRepo = workspaceRepo;
        this.memberRepo = memberRepo;
        this.invitationRepo = invitationRepo;
        this.userRepo = userRepo;
        this.notifSettingRepo = notifSettingRepo;
    }

    /** 내가 속한 워크스페이스 목록 */
    public List<Workspace> getMyWorkspaces(Long userId) {
        return workspaceRepo.findAllByUserId(userId);
    }

    /** 워크스페이스 생성 */
    public Workspace createWorkspace(String name, AppUser owner) {
        Workspace ws = new Workspace();
        ws.setName(name);
        ws.setOwner(owner);
        workspaceRepo.save(ws);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);
        member.setUser(owner);
        member.setRole("OWNER");
        member.setAccepted(true);
        memberRepo.save(member);

        return ws;
    }

    /** 멤버 초대 (이메일 기반 - 토큰 생성) */
    public WorkspaceInvitation inviteMember(Long workspaceId, String email, String role, AppUser inviter) {
        Workspace ws = workspaceRepo.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("워크스페이스를 찾을 수 없습니다"));

        // 권한 확인: OWNER만 초대 가능
        checkPermission(workspaceId, inviter.getId(), "OWNER");

        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setWorkspace(ws);
        invitation.setInvitedEmail(email);
        invitation.setRole(role);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setInvitedBy(inviter);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        return invitationRepo.save(invitation);
    }

    /** 초대 수락 */
    public void acceptInvitation(String token, AppUser user) {
        WorkspaceInvitation inv = invitationRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 초대입니다"));

        if (inv.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("만료된 초대입니다");
        }

        // 이미 수락된 초대인 경우 → 실제 멤버 존재 여부까지 확인
        if (inv.isAccepted()) {
            if (memberRepo.existsByWorkspaceIdAndUserId(inv.getWorkspace().getId(), user.getId())) {
                return; // 진짜 중복 → 조용히 넘어감
            }
            // accepted=true인데 멤버가 없으면 이전 롤백 상태 → 아래로 계속 진행
        }

        // 이미 멤버인지 확인 (다른 경로로 추가된 경우)
        if (memberRepo.existsByWorkspaceIdAndUserId(inv.getWorkspace().getId(), user.getId())) {
            inv.setAccepted(true);
            invitationRepo.save(inv);
            return;
        }

        // 멤버 생성
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(inv.getWorkspace());
        member.setUser(user);
        member.setRole(inv.getRole());
        member.setAccepted(true);
        memberRepo.save(member);

        // 기본 알림 설정 생성 (실패해도 멤버 추가는 유지)
        try {
            NotificationSetting ns = new NotificationSetting();
            ns.setUser(user);
            ns.setWorkspace(inv.getWorkspace());
            notifSettingRepo.save(ns);
        } catch (Exception e) {
            System.err.println("알림 설정 생성 실패 (무시): " + e.getMessage());
        }

        // accepted를 맨 마지막에 설정
        inv.setAccepted(true);
        invitationRepo.save(inv);
    }

    /** 멤버 권한 변경 */
    public void changeMemberRole(Long workspaceId, Long memberId, String newRole, AppUser requester) {
        checkPermission(workspaceId, requester.getId(), "OWNER");

        WorkspaceMember member = memberRepo.findById(memberId)
                .orElseThrow(() -> new RuntimeException("멤버를 찾을 수 없습니다"));

        if (member.getRole().equals("OWNER")) {
            throw new RuntimeException("소유자의 권한은 변경할 수 없습니다");
        }

        member.setRole(newRole);
    }

    /** 멤버 제거 */
    public void removeMember(Long workspaceId, Long memberId, AppUser requester) {
        checkPermission(workspaceId, requester.getId(), "OWNER");

        WorkspaceMember member = memberRepo.findById(memberId)
                .orElseThrow(() -> new RuntimeException("멤버를 찾을 수 없습니다"));

        if (member.getRole().equals("OWNER")) {
            throw new RuntimeException("소유자는 제거할 수 없습니다");
        }

        memberRepo.delete(member);
    }

    /** 멤버 목록 조회 */
    public List<WorkspaceMember> getMembers(Long workspaceId) {
        return memberRepo.findByWorkspaceId(workspaceId);
    }

    /** 권한 확인 유틸 */
    public void checkPermission(Long workspaceId, Long userId, String requiredRole) {
        WorkspaceMember member = memberRepo.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new RuntimeException("이 워크스페이스에 접근 권한이 없습니다"));

        if (!hasPermission(member.getRole(), requiredRole)) {
            throw new RuntimeException("권한이 부족합니다. 필요 권한: " + requiredRole);
        }
    }

    /** OWNER > EDITOR > VIEWER 계층 권한 체크 */
    private boolean hasPermission(String userRole, String requiredRole) {
        int userLevel = roleLevel(userRole);
        int requiredLevel = roleLevel(requiredRole);
        return userLevel >= requiredLevel;
    }

    private int roleLevel(String role) {
        return switch (role) {
            case "OWNER" -> 3;
            case "EDITOR" -> 2;
            case "VIEWER" -> 1;
            default -> 0;
        };
    }

    /** 워크스페이스 삭제 (OWNER만 가능) */
    public void deleteWorkspace(Long workspaceId, AppUser requester) {
        checkPermission(workspaceId, requester.getId(), "OWNER");

        Workspace ws = workspaceRepo.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("워크스페이스를 찾을 수 없습니다"));

        if (!ws.getOwner().getId().equals(requester.getId())) {
            throw new RuntimeException("소유자만 워크스페이스를 삭제할 수 있습니다");
        }

        notifSettingRepo.deleteByWorkspaceId(workspaceId);
        invitationRepo.deleteByWorkspaceId(workspaceId);
        memberRepo.deleteByWorkspaceId(workspaceId);
        workspaceRepo.delete(ws);
    }
}