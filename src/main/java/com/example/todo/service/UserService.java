package com.example.todo.service;

import com.example.todo.domain.AppUser;
import com.example.todo.domain.Workspace;
import com.example.todo.domain.WorkspaceMember;
import com.example.todo.repository.AppUserRepository;
import com.example.todo.repository.WorkspaceMemberRepository;
import com.example.todo.repository.WorkspaceRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final AppUserRepository userRepo;
    private final WorkspaceRepository workspaceRepo;
    private final WorkspaceMemberRepository memberRepo;

    public UserService(AppUserRepository userRepo,
                       WorkspaceRepository workspaceRepo,
                       WorkspaceMemberRepository memberRepo) {
        this.userRepo = userRepo;
        this.workspaceRepo = workspaceRepo;
        this.memberRepo = memberRepo;
    }

    /**
     * Cognito 로그인 후 사용자 자동 등록 (없으면 생성 + 기본 워크스페이스 생성)
     */
    public AppUser getOrCreateUser(OAuth2User oAuth2User) {
        String sub = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        return userRepo.findByCognitoSub(sub).orElseGet(() -> {
            // 신규 사용자 생성
            AppUser user = new AppUser();
            user.setCognitoSub(sub);
            user.setEmail(email);
            user.setName(name != null ? name : email);
            userRepo.save(user);

            // 기본 "내 워크스페이스" 자동 생성
            Workspace ws = new Workspace();
            ws.setName("내 워크스페이스");
            ws.setOwner(user);
            workspaceRepo.save(ws);

            // 본인을 OWNER 멤버로 추가
            WorkspaceMember member = new WorkspaceMember();
            member.setWorkspace(ws);
            member.setUser(user);
            member.setRole("OWNER");
            member.setAccepted(true);
            memberRepo.save(member);

            return user;
        });
    }

    public AppUser findBySubject(String sub) {
        return userRepo.findByCognitoSub(sub).orElseThrow(
                () -> new RuntimeException("사용자를 찾을 수 없습니다: " + sub)
        );
    }
}