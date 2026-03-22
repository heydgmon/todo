[README.md](https://github.com/user-attachments/files/26163692/README.md)
# do·it — 풀스택 일정 관리 서비스

> **Live**: [https://taskall.click](https://taskall.click)  
> **Stack**: Spring Boot 3.4 · JPA · PostgreSQL · AWS (ECS Fargate, CloudFront, Route 53, RDS, Cognito, SES, ECR) · GitHub Actions CI/CD · Docker

비로그인(게스트)과 로그인 모드를 모두 지원하는 일정 관리 웹 서비스입니다.  
Todo · Calendar · Dashboard · Workspace 네 가지 핵심 기능을 하나의 서비스로 통합했습니다.

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [아키텍처](#2-아키텍처)
3. [어플리케이션 — 내가 구현한 것](#3-어플리케이션--내가-구현한-것)
4. [인프라 — 내가 구성한 것](#4-인프라--내가-구성한-것)
5. [트러블슈팅 기록](#5-트러블슈팅-기록)
6. [ERD & 도메인 모델](#6-erd--도메인-모델)
7. [프로젝트 구조](#7-프로젝트-구조)
8. [실행 방법](#8-실행-방법)

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **개발 기간** | 2026.01 ~ 현재 (1인 개발) |
| **서비스 URL** | https://taskall.click |
| **핵심 목표** | Spring Boot + AWS 기반으로 설계부터 배포까지 전 과정을 직접 수행 |
| **주요 키워드** | OAuth2 인증, RBAC 권한 시스템, 듀얼 모드(게스트/로그인), CI/CD 자동화, 이메일 알림 스케줄링 |

---

## 2. 아키텍처

```
사용자
  │
  ▼
Route 53 (taskall.click)
  │
  ▼
CloudFront (HTTPS 종료, ACM 인증서)
  │  Origin: ECS Fargate (HTTP 8080)
  ▼
ECS Fargate (Spring Boot 컨테이너)
  │
  ├──▶ RDS PostgreSQL (데이터 영속화)
  ├──▶ Cognito User Pool (OAuth2 인증)
  ├──▶ SES (이메일 알림 발송)
  └──▶ ECR (Docker 이미지 저장소)

GitHub (master push)
  │
  ▼
GitHub Actions (OIDC → IAM Role)
  │
  ├──  Gradle Build
  ├──  Docker Build & Push → ECR
  └──  ECS Force New Deployment
```

---

## 3. 어플리케이션 — 내가 구현한 것

### 3-1. 듀얼 모드 아키텍처 (게스트 / 로그인)

회원가입 없이 즉시 사용할 수 있도록 **듀얼 모드**를 설계했습니다.

- **게스트 모드**: `LocalStorage` 기반. 서버 통신 없이 브라우저에서 모든 CRUD를 처리하는 `guest-todo.js` 모듈 구현
- **로그인 모드**: `Spring Security + OAuth2` 기반. Cognito를 통한 인증 후 서버 DB(PostgreSQL)에 영속화
- **동일한 UI**: Thymeleaf SSR + JavaScript를 조합하여, 한 페이지에서 `isGuest` 플래그로 분기. 게스트와 로그인 사용자가 동일한 화면을 사용

```java
// 로그인 여부에 따라 서버 렌더링 vs JS 렌더링 분기
window.__loggedIn = /*[[${loggedIn}]]*/ false;

if (GuestTodo.isGuest()) {
    // LocalStorage 기반 렌더링
} else {
    // Thymeleaf 서버 렌더링 그대로 사용
}
```

### 3-2. 인증 & 보안

| 구현 내용 | 상세 |
|-----------|------|
| **OAuth2 + Cognito** | `spring-boot-starter-oauth2-client`로 Cognito User Pool 연동. Authorization Code Grant 방식 |
| **자동 사용자 등록** | `OAuth2LoginSuccessHandler`에서 최초 로그인 시 DB에 사용자 생성 + 기본 워크스페이스 자동 생성 |
| **세션 보안** | `SameSite=Lax`, `Secure=true` 설정. CloudFront HTTPS 환경에 맞춘 쿠키 정책 |
| **API 보호** | `/api/**` 경로에 401 응답 처리. 나머지는 permitAll 후 컨트롤러에서 인증 체크 |

### 3-3. 워크스페이스 & RBAC 권한 시스템

팀 협업을 위한 **멀티 워크스페이스** 구조를 직접 설계했습니다.

- **3단계 계층 권한**: `OWNER > EDITOR > VIEWER`
  - OWNER: 멤버 초대/제거/권한 변경, 워크스페이스 삭제
  - EDITOR: 할 일 CRUD
  - VIEWER: 읽기 전용
- **이메일 초대 시스템**: UUID 토큰 기반 초대 링크 생성 → SES로 발송 → 7일 만료 → 수락 시 멤버 추가
- **권한 체크 유틸**: `checkPermission(workspaceId, userId, requiredRole)` 메서드로 모든 비즈니스 로직에서 통일된 권한 검사

```java
// 계층형 권한 체크: OWNER(3) > EDITOR(2) > VIEWER(1)
private boolean hasPermission(String userRole, String requiredRole) {
    return roleLevel(userRole) >= roleLevel(requiredRole);
}
```

### 3-4. 캘린더 & 일정 관리

- **FullCalendar 연동**: 월간/주간/일간/목록 4가지 뷰. 서버 모드에서는 `/api/calendar/todo` REST API로 이벤트 제공
- **시간 단위 일정**: `startTime`/`endTime` 지원. FullCalendar `extendedProps`에 `sTime`/`eTime`으로 매핑 (예약어 충돌 회피)
- **드래그 앤 드롭**: 캘린더에서 일정을 끌어 날짜 변경 → 서버 API로 자동 반영
- **반복 일정**: 매일/매주/매월 반복 생성 (최대 1년). 개별 삭제 및 반복 전체 삭제 지원
- **장소 연동**: Naver Maps API로 장소 검색 + 지도 클릭 → 위도/경도 저장
- **우선순위 색상 코딩**: HIGH(빨강)/MEDIUM(노랑)/LOW(초록) + 완료 시 회색 취소선

### 3-5. 이메일 알림 스케줄러

`@Scheduled` + AWS SES 기반의 자동 마감 알림 시스템을 구현했습니다.

- **매일 오전 9시 실행**: D-Day, D-1, D-7 세 가지 타이밍으로 미완료 할 일 알림
- **중복 발송 방지**: `NotificationLog` 엔티티로 (todoId, userId, notificationType) 조합의 발송 이력 관리
- **워크스페이스 멤버 전체 알림**: 워크스페이스에 속한 수락된 멤버 전원에게 알림
- **개인별 알림 설정**: `NotificationSetting` 엔티티로 워크스페이스별 알림 on/off 커스터마이징
- **자동 아카이브**: 매일 새벽 3시, 완료 후 3일 경과한 Todo → 아카이브 테이블로 이동 후 삭제
- **로그 자동 정리**: 7일 지난 알림 로그 자동 삭제

```java
@Scheduled(cron = "0 0 9 * * *")  // 매일 오전 9시
public void sendDailyReminders() {
    processReminders(today, "ON_DAY");      // 당일 마감
    processReminders(tomorrow, "DAY_BEFORE"); // D-1
    processReminders(nextWeek, "WEEK_BEFORE"); // D-7
}
```

### 3-6. 도메인 모델 & 데이터 설계

7개의 JPA 엔티티로 도메인을 설계했습니다.

| 엔티티 | 역할 |
|--------|------|
| `Todo` | 할 일 (제목, 마감일, 우선순위, 반복, 장소, 시간, 태그, 부모-자식 관계) |
| `AppUser` | 사용자 (Cognito sub 연동) |
| `Workspace` | 워크스페이스 (소유자, 멤버 목록) |
| `WorkspaceMember` | 멤버십 (워크스페이스-사용자 N:M, 역할, 수락 여부) |
| `WorkspaceInvitation` | 초대 (토큰, 만료 시간, 수락 여부) |
| `NotificationSetting` | 알림 설정 (사용자별 워크스페이스별 on/off) |
| `NotificationLog` | 발송 이력 (중복 방지용 유니크 제약) |

---

## 4. 인프라 — 내가 구성한 것

### 4-1. CI/CD 파이프라인 (GitHub Actions → ECS)

`master` 브랜치 push 시 자동으로 빌드 → 배포가 실행됩니다.

```
master push
    ↓
GitHub Actions
    ├── 1. JDK 17 + Gradle Build
    ├── 2. OIDC로 AWS IAM Role Assume (Access Key 없음)
    ├── 3. Docker Build → ECR Push
    └── 4. ECS Force New Deployment
```

- **OIDC 인증**: GitHub Actions에서 AWS에 Access Key 없이 IAM Role을 Assume. `id-token: write` 퍼미션으로 OIDC 토큰 발급
- **ECR 이미지 관리**: `eclipse-temurin:17-jre` 기반 경량 Docker 이미지
- **무중단 배포**: ECS `force-new-deployment`로 롤링 업데이트

### 4-2. AWS 인프라 구성

| 서비스 | 용도 | 설정 |
|--------|------|------|
| **ECS Fargate** | 컨테이너 실행 | `todo-cluster` / `todo-service` |
| **ECR** | Docker 이미지 저장 | `604227045332.dkr.ecr.ap-northeast-2.amazonaws.com/todo` |
| **CloudFront** | CDN + HTTPS 종료 | Origin → ECS Fargate (HTTP 8080) |
| **RDS** | PostgreSQL DB | `spring.jpa.hibernate.ddl-auto=validate` |
| **Cognito** | 사용자 인증 | User Pool + OAuth2 Authorization Code |
| **SES** | 이메일 발송 | `noreply@taskall.click` 발신 |
| **Route 53** | DNS · 도메인 관리 | `taskall.click` → CloudFront Distribution |
| **ACM** | SSL/TLS 인증서 | CloudFront에 연결 |
| **IAM** | 권한 관리 | GitHub Actions OIDC Role, ECS Task Role |

### 4-3. CloudFront + HTTPS + OAuth2 콜백 처리

CloudFront(HTTPS) → ECS(HTTP) 환경에서 OAuth2 콜백 URL이 내부 주소로 생성되는 문제를 해결했습니다.

```
문제: 사용자 → https://taskall.click → CloudFront → http://ECS:8080
      Spring이 자신을 http://내부IP:8080으로 인식
      → OAuth2 redirect_uri 불일치 → authorization_request_not_found

해결:
  1. ForwardedHeaderFilter 등록 → X-Forwarded-Proto/Host 헤더 처리
  2. server.forward-headers-strategy=native
  3. Tomcat remoteip 설정으로 CloudFront 프록시 신뢰
  4. 세션 쿠키 SameSite=Lax, Secure=true
```

### 4-4. Docker

```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

- JRE 전용 이미지로 경량화
- 환경변수(`DB_URL`, `DB_USER`, `DB_PASSWORD`)를 ECS Task Definition에서 주입

---

## 5. 트러블슈팅 기록

### CloudFront 뒤에서 OAuth2 `authorization_request_not_found`

**증상**: Cognito 로그인 후 콜백에서 `authorization_request_not_found` 에러

**원인**: CloudFront가 HTTPS를 종료하고 HTTP로 Origin(ECS)에 포워딩하면서 Spring이 내부 주소(`http://내부IP:8080`)로 redirect_uri를 생성. Cognito에 등록된 `https://taskall.click/login/oauth2/code/cognito`와 불일치.

**해결**:
1. `ForwardedHeaderFilter` Bean 등록 → X-Forwarded-* 헤더 처리
2. `server.forward-headers-strategy=native` 설정
3. `server.tomcat.remoteip.internal-proxies=.*` 로 CloudFront를 신뢰 프록시로 등록
4. redirect-uri를 명시적으로 `https://taskall.click/...`로 고정

### FullCalendar `startTime`/`endTime` 예약어 충돌

**증상**: `extendedProps`에 `startTime`을 넣으면 FullCalendar가 매일 반복 이벤트로 해석

**원인**: `startTime`/`endTime`은 FullCalendar의 recurring event 예약어

**해결**: 서버 API와 게스트 모드 모두에서 `sTime`/`eTime`으로 이름 변경하여 `extendedProps`에 전달

### ECS 헬스체크 실패

**증상**: ECS Task가 반복적으로 종료됨

**원인**: CloudFront/ECS 헬스체크 경로에 인증이 필요한 상태

**해결**: `spring-boot-starter-actuator` 추가 + `management.endpoints.web.exposure.include=health` 설정. SecurityConfig에서 `anyRequest().permitAll()`로 헬스체크 경로 허용

---

## 6. ERD & 도메인 모델

```
app_user ──┐
           │ 1:N
           ▼
      workspace ─────────────────┐
           │                     │
           │ 1:N                 │ 1:N
           ▼                     ▼
   workspace_member    workspace_invitation
           │
           │ (workspace_id)
           ▼
         todo ──── todo_tags (ElementCollection)
           │
           ├── notification_setting (user + workspace별)
           └── notification_log (중복 발송 방지)
```

---

## 7. 프로젝트 구조

```
src/main/java/com/example/todo/
├── config/
│   ├── SecurityConfig.java          # Spring Security + OAuth2 설정
│   ├── OAuth2LoginSuccessHandler.java # 로그인 성공 시 사용자 자동 등록
│   ├── WebConfig.java               # ForwardedHeaderFilter (CloudFront 대응)
│   ├── CurrentUserHelper.java       # 현재 사용자 추출 유틸
│   └── AppConfig.java               # @EnableScheduling
│
├── domain/
│   ├── Todo.java                    # 할 일 엔티티 (장소, 시간, 태그, 반복)
│   ├── AppUser.java                 # 사용자 (Cognito sub)
│   ├── Workspace.java               # 워크스페이스
│   ├── WorkspaceMember.java         # 멤버십 (RBAC)
│   ├── WorkspaceInvitation.java     # 이메일 초대
│   ├── NotificationSetting.java     # 알림 설정
│   └── NotificationLog.java         # 발송 이력
│
├── repository/                      # Spring Data JPA (JPQL 커스텀 쿼리 포함)
│
├── service/
│   ├── TodoService.java             # 할 일 CRUD + 장소/시간 처리
│   ├── WorkspaceService.java        # 워크스페이스 + RBAC 권한 체크
│   ├── UserService.java             # 사용자 자동 등록 + 기본 워크스페이스 생성
│   ├── SesEmailService.java         # AWS SES 이메일 발송
│   └── ReminderScheduler.java       # @Scheduled 알림 + 아카이브 + 로그 정리
│
├── controller/
│   ├── TodoController.java          # /todo, /todoNew (캘린더 연동)
│   ├── CalendarController.java      # /api/calendar/todo (REST)
│   ├── CalendarPageController.java  # /calendar (페이지)
│   ├── DashboardController.java     # /dashboard
│   ├── WorkspaceController.java     # /workspace (CRUD + 초대 + 멤버 관리)
│   └── HomeController.java          # / (랜딩 페이지)
│
src/main/resources/
├── templates/                       # Thymeleaf 템플릿
│   ├── index.html                   # 랜딩 페이지
│   ├── dashboard.html               # 대시보드
│   ├── calendar.html                # 캘린더 (FullCalendar)
│   ├── todo.html                    # 할 일 관리
│   ├── workspace/                   # 워크스페이스 관련 페이지
│   ├── guide/                       # 사용 가이드 8개 페이지
│   └── fragments/                   # 공통 컴포넌트 (sidebar, footer)
│
├── static/
│   ├── js/guest-todo.js             # 게스트 모드 LocalStorage 모듈
│   └── css/                         # 스타일시트
│
└── application.properties           # DB, Cognito, SES, CloudFront 설정

.github/workflows/deploy.yml         # CI/CD (GitHub Actions → ECR → ECS)
Dockerfile                            # 컨테이너 이미지 정의
buildspec.yml                         # CodeBuild (레거시, 현재 미사용)
todo-deploy.yaml / todo-svc.yaml      # K8s 매니페스트 (EKS 실험용)
```

---

## 8. 실행 방법

### 로컬 실행

```bash
# 1. PostgreSQL 준비 후 환경변수 설정
export DB_URL=jdbc:postgresql://localhost:5432/todo
export DB_USER=postgres
export DB_PASSWORD=yourpassword

# 2. 빌드 & 실행
./gradlew clean build -x test
java -jar build/libs/*.jar
```

### Docker 실행

```bash
./gradlew clean build -x test
docker build -t todo .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/todo \
  -e DB_USER=postgres \
  -e DB_PASSWORD=yourpassword \
  todo
```

---

> 이 프로젝트는 설계 → 백엔드 → 프론트엔드 → 인프라 → CI/CD → 운영까지 전 과정을 1인으로 수행한 풀스택 포트폴리오입니다.
