package com.example.todo.controller;

import com.example.todo.config.CurrentUserHelper;
import com.example.todo.domain.*;
import com.example.todo.service.SesEmailService;
import com.example.todo.service.WorkspaceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/workspace")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final CurrentUserHelper userHelper;
    private final SesEmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    public WorkspaceController(WorkspaceService workspaceService,
                               CurrentUserHelper userHelper,
                               SesEmailService emailService) {
        this.workspaceService = workspaceService;
        this.userHelper = userHelper;
        this.emailService = emailService;
    }

    /** 워크스페이스 목록 */
    @GetMapping
    public String list(Authentication auth, Model model) {
        AppUser user = userHelper.getCurrentUser(auth);
        if (user == null) {
            return "redirect:/oauth2/authorization/cognito";
        }
        List<Workspace> workspaces = workspaceService.getMyWorkspaces(user.getId());
        model.addAttribute("workspaces", workspaces);
        return "workspace/list";
    }

    /** 워크스페이스 생성 */
    @PostMapping("/create")
    public String create(@RequestParam String name, Authentication auth) {
        AppUser user = userHelper.getCurrentUser(auth);
        if (user == null) {
            return "redirect:/oauth2/authorization/cognito";
        }
        workspaceService.createWorkspace(name, user);
        return "redirect:/workspace";
    }

    /** 워크스페이스 삭제 */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth) {
        AppUser user = userHelper.getCurrentUser(auth);
        if (user == null) {
            return "redirect:/oauth2/authorization/cognito";
        }
        workspaceService.deleteWorkspace(id, user);
        return "redirect:/workspace";
    }

    /** 워크스페이스 멤버 관리 페이지 */
    @GetMapping("/{id}/members")
    public String members(@PathVariable Long id, Authentication auth, Model model) {
        AppUser user = userHelper.getCurrentUser(auth);
        if (user == null) {
            return "redirect:/oauth2/authorization/cognito";
        }

        workspaceService.checkPermission(id, user.getId(), "VIEWER");

        List<WorkspaceMember> members = workspaceService.getMembers(id);
        model.addAttribute("members", members);
        model.addAttribute("workspaceId", id);
        return "workspace/members";
    }

    /** 멤버 초대 */
    @PostMapping("/{id}/invite")
    public String invite(@PathVariable Long id,
                         @RequestParam String email,
                         @RequestParam String role,
                         Authentication auth) {

        AppUser user = userHelper.getCurrentUser(auth);
        if (user == null) {
            return "redirect:/oauth2/authorization/cognito";
        }
        WorkspaceInvitation inv = workspaceService.inviteMember(id, email, role, user);

        emailService.sendInvitationEmail(
                email,
                inv.getWorkspace().getName(),
                user.getName(),
                inv.getToken(),
                baseUrl
        );

        return "redirect:/workspace/" + id + "/members";
    }

    /** 초대 수락 */
    @GetMapping("/invite/accept")
    public String acceptInvitation(@RequestParam String token, Authentication auth,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        AppUser user = userHelper.getCurrentUser(auth);
        if (user == null) {
            request.getSession().setAttribute("redirectUrl",
                    "/workspace/invite/accept?token=" + token);
            return "redirect:/oauth2/authorization/cognito";
        }
        try {
            workspaceService.acceptInvitation(token, user);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/workspace";
    }

    /** 멤버 권한 변경 */
    @PostMapping("/{wsId}/members/{memberId}/role")
    public String changeRole(@PathVariable Long wsId,
                             @PathVariable Long memberId,
                             @RequestParam String role,
                             Authentication auth) {
        AppUser user = userHelper.getCurrentUser(auth);
        if (user == null) {
            return "redirect:/oauth2/authorization/cognito";
        }
        workspaceService.changeMemberRole(wsId, memberId, role, user);
        return "redirect:/workspace/" + wsId + "/members";
    }

    /** 멤버 제거 */
    @PostMapping("/{wsId}/members/{memberId}/remove")
    public String removeMember(@PathVariable Long wsId,
                               @PathVariable Long memberId,
                               Authentication auth) {
        AppUser user = userHelper.getCurrentUser(auth);
        if (user == null) {
            return "redirect:/oauth2/authorization/cognito";
        }
        workspaceService.removeMember(wsId, memberId, user);
        return "redirect:/workspace/" + wsId + "/members";
    }
}