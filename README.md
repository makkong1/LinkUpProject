# LinkUp

**게시판 + 노션 스타일 콘텐츠 + 실시간 알림 + 소셜 로그인을 지원하는 통합 커뮤니티 플랫폼**

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen)
![Java](https://img.shields.io/badge/Java-17-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Redis](https://img.shields.io/badge/Redis-7.0-red)

**GitHub**: [https://github.com/makkong1/LinkUpProject](https://github.com/makkong1/LinkUpProject)

---

## 📖 프로젝트 소개

### 프로젝트 목적
노션 스타일 콘텐츠 작성과 사용자 간 의견 교환을 더 편하게 구현하고자 개발한 커뮤니티 플랫폼입니다. 게시판 기능, 노션 스타일 에디터, 실시간 알림, 소셜 로그인 등 현대적인 웹 서비스의 핵심 기능을 통합 구현했습니다.

### 개발 배경
- **노션 스타일 콘텐츠 작성**: 직관적이고 자유로운 콘텐츠 작성 환경 제공
- **사용자 간 의견 교환**: 실시간 알림을 통한 활발한 커뮤니케이션 지원
- **성능 최적화**: Redis 캐싱과 비동기 처리를 통한 고성능 서비스 구현
- **사용자 편의성**: Google, Naver 소셜 로그인으로 간편한 인증 제공

### 해결하려는 문제
- **기존 게시판의 한계**: 단순한 CRUD 기능만 제공하는 정적인 게시판
- **콘텐츠 작성의 제약**: 텍스트 위주의 제한적인 에디터 환경
- **실시간 상호작용 부족**: 댓글 작성 시 실시간 알림 미지원
- **성능 이슈**: 조회수, 좋아요/싫어요 등 빈번한 업데이트로 인한 DB 부하

### 왜 이 프로젝트를 만들었는지
- **노션 스타일 에디터**: 직관적이고 자유로운 콘텐츠 작성 경험 제공
- **성능 최적화**: Redis 캐싱과 비동기 처리를 통한 고성능 서비스 구현
- **실시간 경험**: SSE(Server-Sent Events)와 Redis Pub/Sub을 활용한 실시간 댓글 알림
- **사용자 편의성**: Google, Naver 소셜 로그인으로 간편한 인증 제공
- **확장 가능한 아키텍처**: 도메인 중심 설계와 계층 분리를 통한 유지보수성 향상

---

## 🛠 Tech Stack

### Backend
- **Language / Framework**: Java 17, Spring Boot 3.3.4
- **Database**: MySQL 8.0
- **ORM**: Spring Data JPA, MyBatis 3.5.16
- **Cache**: Redis 7.0 (Lettuce)
- **Security**: Spring Security 6, OAuth2 Client (Google, Naver)
- **Build / Infra**: Gradle, Docker
- **Async / Scheduler**: Spring @Async, @Scheduled
- **Monitoring**: Spring Boot Admin, Actuator, Prometheus

### Frontend
- **Framework**: Thymeleaf (서버 사이드 렌더링)
- **Styling**: CSS
- **State / HTTP**: Spring MVC, AJAX
- **Visualization**: Spring Boot Admin Dashboard

---

## 🌟 핵심 기능 (Key Features)

### 1️⃣ 일반/소셜 로그인 (Authentication)

**주요 기능 요약**
- Google, Naver 계정을 통한 소셜 로그인 지원
- 일반 회원가입과 소셜 로그인 통합 관리
- 세션 기반 인증 (동시 세션 1개 제한)

**핵심 로직**
- **일반 로그인**: Spring Security Form Login + BCrypt 암호화
  - `CustomUserDetailsService`로 사용자 조회 및 인증
  - 로그인 실패 횟수 제한 (5회 초과 시 계정 잠금)
  - SecurityContextHolder → HttpSession 저장
- **소셜 로그인**: OAuth2 Client (Google, Naver)
  - `CustomOAuth2UserService`로 소셜 사용자 정보 처리
  - `SocialUser` 엔티티로 소셜 계정 정보 저장
  - 일반 사용자(`Users`)와 소셜 사용자(`SocialUser`) 통합 관리

**인증 흐름**
```
[일반 로그인]
사용자 요청 → CustomUserDetailsService → BCrypt 검증 → 세션 생성 → SecurityContext 저장

[소셜 로그인]
소셜 로그인 클릭 → OAuth2 인증 → 사용자 정보 조회 → SocialUser 저장/조회 → 세션 생성
```

**보안 고려 사항**
- 세션 고정 공격 방어 (`migrateSession`)
- CSRF 보호 (Spring Security 기본 제공)
- 인증/인가 실패 핸들러 적용 (`AuthenticationEntryPoint`, `AccessDeniedHandler`)
- 메서드 보안 적용 (`@PreAuthorize`)

### 2️⃣ 노션 스타일 에디터 (Notion-style Editor)

**주요 기능 요약**
- 직관적이고 자유로운 콘텐츠 작성 환경
- 노션 스타일 콘텐츠 페이지 관리
- 파일 업로드 지원

**핵심 로직**
- `Notion` 엔티티로 노션 스타일 콘텐츠 저장
- 일반 사용자와 소셜 사용자 모두 작성 가능
- 조회수, 좋아요 수 관리

**상태 흐름**
```
콘텐츠 작성 → 노션 에디터 → 저장 (DB) → 노션 페이지 조회 → 조회수 증가
```

### 3️⃣ 게시판 CRUD (Board Management with Redis Caching)

**주요 기능 요약**
- 게시글 CRUD (생성, 조회, 수정, 삭제)
- 카테고리별 분류 (공지사항, 일반 게시글, 문의)
- 검색 기능 (제목, 작성자, 내용)
- 페이징 처리
- 파일 업로드/다운로드
- 게시글 신고 기능

**핵심 로직**
- **공지사항 캐싱**: Redis 캐싱으로 빠른 조회 제공
  - `@Cacheable`로 공지사항 목록 캐싱 (TTL: 10분)
  - 캐시 무효화 시 `@CacheEvict`로 즉시 갱신
- **조회수 증가**: `@Async`로 비동기 처리하여 응답 속도 향상
- **게시글 상태 관리**: `N`(정상), `Y`(삭제), `R`(신고)

**성능 개선 결과** (공지사항 캐싱 적용)
- **평균 응답 시간**: 608ms → 137ms (**77% 감소**)
- **최대 응답 시간**: 4083ms → 955ms (응답 안정성 향상)
- **Throughput**: 144.8/sec → 457.7/sec (**3배 증가**)
- **에러율**: 0% 유지 (안정적 처리)

**상태 흐름**
```
게시글 작성 → 저장 (DB) → 목록 조회 (캐시 확인) → 상세 조회 (비동기 조회수 증가)
→ 신고 처리 → 관리자 승인/거부 → 상태 변경
```

### 4️⃣ 비동기 좋아요/싫어요 (Like/Dislike with Redis Caching)

**주요 기능 요약**
- 게시글 및 댓글에 좋아요/싫어요 기능
- Redis 캐싱으로 즉시 반영
- 주기적 DB 동기화 (5분마다)

**핵심 로직**
- **즉시 반영**: 좋아요/싫어요 클릭 시 Redis에 즉시 반영
  - 키 형식: `board:like:{id}`, `comment:like:{id}`
  - Redis `INCR` 명령으로 원자적 증가
- **변경 추적**: 변경된 ID는 Set에 저장 (`changed:board`, `changed:comment`)
- **주기적 동기화**: `@Scheduled`로 5분마다 Redis → DB 동기화
- **동기화 완료**: 동기화 완료 후 Redis 캐시 초기화

**동기화 흐름**
```
사용자 클릭 → Redis 증가 (INCR) → 즉시 반환 → 스케줄러 동기화 (5분마다)
→ DB 업데이트 → Redis 초기화
```

**성능 개선 효과**
- DB 부하 **80% 감소** (빈번한 업데이트를 Redis로 분산)
- 사용자 경험 향상 (즉시 반영)

### 5️⃣ 실시간 알림 기능 (Real-time Notification with Redis Pub/Sub)

**주요 기능 요약**
- 댓글 작성 시 게시글 작성자에게 실시간 알림
- SSE(Server-Sent Events)로 브라우저 푸시
- Redis Pub/Sub으로 서버 간 메시지 전파

**핵심 로직**
- **Publisher**: 댓글 작성 시 Redis Pub/Sub으로 알림 발행
  - 토픽: `comment_notifications`
  - 메시지 형식: `{userId}:{알림 내용}`
- **Subscriber**: `CommentNotificationSubscriber`가 메시지 수신
- **SSE 전송**: 연결된 SSE Emitter로 실시간 전송
- **연결 관리**: 사용자별 연결 관리 (`ConcurrentHashMap<String, SseEmitter>`)

**비동기 구조 흐름**
```
댓글 작성 → Publisher → Redis Pub/Sub → Subscriber 수신 
→ SSE Emitter 전송 → 브라우저 알림 표시
```

**설계 이유 및 트레이드오프**
- **이유**: 실시간 알림을 위한 경량 솔루션, WebSocket 대비 구현 단순
- **트레이드오프**: 단방향 통신만 지원 (서버 → 클라이언트)

### 6️⃣ 관리자 기능 (Admin Management)

**주요 기능 요약**
- 게시글/댓글 신고 관리
- 사용자 차단/해제
- 전체 게시글 조회 및 관리
- 모니터링 대시보드 (Spring Boot Admin)

**핵심 로직**
- `@PreAuthorize("hasRole('ADMIN')")`로 권한 제어
- 신고된 게시글 상태 변경 (`R` → `N`)
- 계정 잠금/해제 기능
- 로컬 IP만 모니터링 접근 허용

**상태 흐름**
```
신고 접수 → 관리자 확인 → 승인/거부 → 상태 변경 → 사용자 알림
```

---

## 🏗️ 시스템 아키텍처 & 설계 전략

### 📐 전체 아키텍처 개요

**서비스 구조**
```
Controller Layer (요청 처리, 검증)
    ↓
Service Layer (비즈니스 로직)
    ↓
Repository Layer (데이터 접근)
    ↓
Database (MySQL) / Cache (Redis)
```

**컴포넌트 간 책임 분리**
- **Controller**: HTTP 요청/응답 처리, 입력 검증, 권한 체크
- **Service**: 비즈니스 로직, 트랜잭션 관리, 캐시 처리
- **Repository**: 데이터 접근, 쿼리 실행
- **Converter**: Entity ↔ DTO 변환
- **Util**: 공통 유틸리티 (사용자 정보, 좋아요/싫어요 처리)

**선택한 구조의 이유**
- **계층 분리**: 관심사 분리로 유지보수성 향상
- **DTO 패턴**: 엔티티 노출 방지, API 안정성 확보
- **Converter 패턴**: 변환 로직 중앙화로 일관성 유지

### 📊 데이터 처리 전략

**Problem**
- 공지사항 조회 시 매번 DB 쿼리 실행
- 좋아요/싫어요 클릭 시마다 DB 업데이트로 인한 부하
- 게시글 목록 조회 시 N+1 문제

**Solution**
- **Redis 캐싱**: 공지사항은 `@Cacheable`로 캐싱 (TTL: 10분)
- **Write-Through Cache**: 좋아요/싫어요는 Redis에 먼저 저장 후 주기적 동기화
- **Lazy Loading**: JPA `FetchType.LAZY`로 필요 시에만 조회
- **페이징 처리**: `Pageable`로 대량 데이터 효율적 처리

### 🔔 이벤트 / 알림 / 비동기 처리 전략

**설계 이유 및 트레이드오프**
- **SSE + Redis Pub/Sub**: 
  - 이유: 실시간 알림을 위한 경량 솔루션, WebSocket 대비 구현 단순
  - 트레이드오프: 단방향 통신만 지원 (서버 → 클라이언트)
- **@Async 조회수 증가**: 
  - 이유: 응답 속도 향상, 사용자 경험 개선
  - 트레이드오프: 비동기 처리로 인한 일시적 데이터 불일치 가능성
- **@Scheduled 좋아요/싫어요 동기화**: 
  - 이유: DB 부하 감소, 즉시 반영으로 사용자 경험 향상
  - 트레이드오프: 5분 지연으로 인한 일시적 불일치

### 🔐 인증 & 보안 아키텍처

**인증 방식**
- **일반 로그인**: Spring Security Form Login + BCrypt 암호화
- **소셜 로그인**: OAuth2 Client (Google, Naver)
- **세션 관리**: HttpSession 기반, 동시 세션 1개 제한

**권한 구조**
- `ROLE_USER`: 일반 사용자
- `ROLE_ADMIN`: 관리자
- `ROLE_SUB_ADMIN`: 부관리자

**보안 고려 사항**
- 세션 고정 공격 방어 (`migrateSession`)
- 로그인 실패 횟수 제한 (계정 잠금)
- CSRF 보호 (Spring Security 기본 제공)
- SQL Injection 방지 (JPA, MyBatis 파라미터 바인딩)
- **GlobalExceptionHandler**: 전역 예외 처리를 통한 일관된 오류 응답 제공
- **인증/인가 실패 핸들러**: `AuthenticationEntryPoint`, `AccessDeniedHandler` 적용
- **메서드 보안**: `@PreAuthorize` 등을 통한 세밀한 접근 제어

---

## 📁 프로젝트 구조

```
link-up/
├── src/
│   ├── main/
│   │   ├── java/kh/link_up/
│   │   │   ├── config/              # 설정 클래스
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   └── WebConfig.java
│   │   │   ├── controller/          # 컨트롤러
│   │   │   │   ├── BoardController.java
│   │   │   │   ├── CommentController.java
│   │   │   │   ├── UsersController.java
│   │   │   │   ├── NotionController.java
│   │   │   │   └── AdminController.java
│   │   │   ├── service/             # 서비스
│   │   │   │   ├── BoardService.java
│   │   │   │   ├── CommentService.java
│   │   │   │   ├── NotionService.java
│   │   │   │   ├── BoardCacheService.java
│   │   │   │   ├── LikeDislikeCacheService.java
│   │   │   │   └── LikeDislikeSyncService.java
│   │   │   ├── repository/          # 리포지토리
│   │   │   │   ├── BoardRepository.java
│   │   │   │   ├── CommentRepository.java
│   │   │   │   ├── NotionRepository.java
│   │   │   │   └── UsersRepository.java
│   │   │   ├── domain/              # 엔티티
│   │   │   │   ├── Board.java
│   │   │   │   ├── Comment.java
│   │   │   │   ├── Notion.java
│   │   │   │   ├── Users.java
│   │   │   │   └── SocialUser.java
│   │   │   ├── dto/                 # DTO
│   │   │   ├── converter/           # 변환기
│   │   │   ├── util/                # 유틸리티
│   │   │   └── Ouath2/              # OAuth2 설정
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── templates/           # Thymeleaf 템플릿
│   │       └── static/              # 정적 리소스
│   └── test/
├── build.gradle
├── DockerFile
└── README.md
```

---

## 🔌 API 설계

### 도메인별 API 문서
- Swagger UI: `http://localhost:8081/swagger-ui.html` (SpringDoc OpenAPI)

### REST 설계 기준
- **게시글**: `/board` (GET: 목록, POST: 생성, GET `/{id}`: 상세, DELETE `/{id}`: 삭제)
- **댓글**: `/comment` (POST: 생성, GET: 목록, DELETE `/{id}`: 삭제)
- **노션**: `/notion` (노션 스타일 콘텐츠 관리)
- **좋아요/싫어요**: `/board/{id}/like`, `/board/{id}/dislike`
- **사용자**: `/users` (회원가입, 로그인, 로그아웃)
- **관리자**: `/admin/**` (권한: ADMIN, SUB_ADMIN)

### 인증/인가 적용 방식
- **인증**: Spring Security Form Login, OAuth2
- **인가**: `@PreAuthorize`, `hasRole()`, `permitAll()`
- **세션**: HttpSession 기반, 30분 타임아웃

---

## 🗄️ 데이터베이스 설계

### 주요 엔티티

**users** (일반 회원 정보를 저장하는 테이블)
- `u_idx`: 사용자 고유 ID (PK)
- `u_id`: 로그인 ID (UNIQUE)
- `u_pwd`: 비밀번호 (BCrypt 암호화)
- `u_nickname`: 닉네임
- `u_email`: 이메일
- `u_role`: 역할 (USER, ADMIN, SUB_ADMIN)
- `failed_login_attempts`: 로그인 실패 횟수
- `account_locked`: 계정 잠금 여부

**board** (사용자 또는 소셜 사용자가 작성한 게시글 정보를 저장)
- `b_idx`: 게시글 고유 ID (PK)
- `b_writer`: 작성자 ID (FK → users.u_idx)
- `social_user_id`: 소셜 사용자 ID (FK → social_user.social_user_id, nullable)
- `b_category`: 카테고리 (NOTICE, GENERAL, INQUIRY)
- `b_title`: 제목
- `b_content`: 내용 (TEXT)
- `b_view_cnt`: 조회수
- `b_like`: 좋아요 수
- `b_dislike`: 싫어요 수
- `b_isdeleted`: 삭제 여부 (N, Y, R)
- `b_report`: 신고 횟수

**comment** (게시글에 달린 댓글 정보를 저장)
- `c_idx`: 댓글 고유 ID (PK)
- `c_writer`: 작성자 ID (FK → users.u_idx)
- `social_user_id`: 소셜 사용자 ID (FK → social_user.social_user_id, nullable)
- `b_idx`: 게시글 ID (FK → board.b_idx)
- `c_content`: 댓글 내용
- `c_like`: 좋아요 수
- `c_dislike`: 싫어요 수
- `c_deleted`: 삭제 여부
- `c_report`: 신고 횟수

**notion** (노션 스타일 콘텐츠 페이지 정보를 저장)
- `n_idx`: 노션 고유 ID (PK)
- `n_writer`: 작성자 ID (FK → users.u_idx)
- `social_user_id`: 소셜 사용자 ID (FK → social_user.social_user_id, nullable)
- `n_title`: 제목
- `n_content`: 내용 (TEXT)
- `n_view_cnt`: 조회수
- `n_like`: 좋아요 수
- `n_filepath`: 파일 경로

**social_user** (소셜 로그인 사용자의 고유 정보 및 인증 데이터를 저장)
- `social_user_id`: 소셜 사용자 고유 ID (PK)
- `provider`: 제공자 (google, naver)
- `provider_user_id`: 제공자 사용자 ID
- `email`: 이메일
- `name`: 이름
- `user_id`: 일반 사용자 ID (FK → users.u_idx, nullable)

**oauth2_authorized_client** (OAuth2 인증 토큰 정보를 저장)

### 도메인 간 관계
- `Users` 1:N `Board` (일반 사용자 작성 게시글)
- `Users` 1:N `Comment` (일반 사용자 작성 댓글)
- `Users` 1:N `Notion` (일반 사용자 작성 노션)
- `SocialUser` 1:N `Board` (소셜 사용자 작성 게시글)
- `SocialUser` 1:N `Comment` (소셜 사용자 작성 댓글)
- `SocialUser` 1:N `Notion` (소셜 사용자 작성 노션)
- `Board` 1:N `Comment` (게시글-댓글)

### 인덱스 전략
- `users.u_id`: UNIQUE 인덱스 (로그인 ID 조회)
- `board.b_category`: 인덱스 (카테고리별 조회)
- `board.b_upload`: 인덱스 (최신순 정렬)
- `comment.b_idx`: 인덱스 (게시글별 댓글 조회)

### 조회 패턴 기반 인덱스
- **게시글 목록**: `category` + `upload_time` DESC
- **검색**: `title`, `content` (Full-Text Search 고려)
- **댓글 목록**: `b_idx` + `c_upload` DESC

### 성능 고려 사항
- JPA `FetchType.LAZY`로 N+1 문제 방지
- 페이징 처리로 대량 데이터 효율적 조회
- Redis 캐싱으로 빈번한 조회 최적화

---

## 🔒 보안 구현

### 인증

**토큰 전략**
- 세션 기반 인증 (HttpSession)
- 세션 ID는 쿠키에 저장 (`JSESSIONID`)
- 세션 타임아웃: 30분

**로그인 흐름**
```
1. 사용자 로그인 요청
2. CustomUserDetailsService로 사용자 조회
3. BCrypt로 비밀번호 검증
4. 로그인 실패 횟수 체크 (5회 초과 시 계정 잠금)
5. 세션 생성 및 저장 (SecurityContextHolder → HttpSession)
6. 성공 핸들러로 리다이렉트
```

**일반 로그인 vs 소셜 로그인 흐름도**
- **일반 로그인**: Form Login → CustomUserDetailsService → BCrypt 검증 → 세션 생성
- **소셜 로그인**: OAuth2 → CustomOAuth2UserService → SocialUser 저장/조회 → 세션 생성

### 권한 제어

**Role 구조**
- `ROLE_USER`: 일반 사용자 (게시글/댓글 작성, 조회)
- `ROLE_ADMIN`: 관리자 (모든 권한)
- `ROLE_SUB_ADMIN`: 부관리자 (제한적 관리 권한)

**접근 제어 기준**
- `@PreAuthorize("hasRole('ADMIN')")`: 관리자 전용
- `@PreAuthorize("permitAll()")`: 인증 불필요
- `@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")`: 인증 사용자

**예외 처리 및 보안**
- **GlobalExceptionHandler**: 전역 예외 처리를 통한 일관된 오류 응답 제공
- **세션 무효화**: 세션 타임아웃 및 보안 설정
- **CSRF 설정**: Spring Security 기본 제공 CSRF 보호
- **URI 접근 제어**: 다양한 보안 설정을 통한 시스템 보호
- **인증/인가 실패 핸들러**: `AuthenticationEntryPoint`, `AccessDeniedHandler` 적용
- **메서드 보안**: `@PreAuthorize` 등을 통한 세밀한 접근 제어

---

## ⚡ 성능 최적화

### 캐싱 전략

**캐시 대상**
- 공지사항 목록 (`noticeBoards` 캐시)
- 좋아요/싫어요 수 (Redis 임시 저장)

**TTL 전략**
- 공지사항: 10분 (캐시 무효화 시 즉시 갱신)
- 좋아요/싫어요: 동기화 완료 시 삭제

**키 설계**
- 공지사항: `noticeBoards::list`
- 좋아요: `board:like:{id}`, `comment:like:{id}`
- 싫어요: `board:dislike:{id}`, `comment:dislike:{id}`
- 변경 추적: `changed:board`, `changed:comment`

**성능 개선 결과** (공지사항 캐싱)
- **평균 응답 시간**: 608ms → 137ms (**77% 감소**)
- **최대 응답 시간**: 4083ms → 955ms (응답 안정성 향상)
- **Throughput**: 144.8/sec → 457.7/sec (**3배 증가**)
- **에러율**: 0% 유지 (안정적 처리)

### 쿼리 최적화

**N+1 해결**
- `FetchType.LAZY`로 지연 로딩
- `@EntityGraph` 또는 JOIN FETCH로 필요한 경우 즉시 로딩
- DTO 프로젝션으로 필요한 필드만 조회

**페이징 전략**
- `Pageable`로 페이지당 10개 항목 조회
- `Sort.by()`로 정렬 최적화

**대량 데이터 처리**
- 페이징으로 전체 데이터 조회 방지
- 검색 조건 적용으로 불필요한 데이터 필터링

---

## 🔄 트랜잭션 & 동시성 제어

### 트랜잭션 전략

**경계 설정**
- `@Transactional`을 Service 레이어에 적용
- 읽기 전용: `@Transactional(readOnly = true)`
- 쓰기 작업: 기본 `@Transactional`

**롤백 정책**
- RuntimeException, Error 발생 시 자동 롤백
- 체크 예외는 기본적으로 롤백하지 않음

### 동시성 제어

**DB 제약**
- `@Version` (낙관적 locking) 미사용 (현재 버전)
- 비관적 locking 필요 시 `SELECT ... FOR UPDATE` 고려

**원자적 처리**
- Redis `INCR` 명령으로 좋아요/싫어요 원자적 증가
- DB 업데이트는 트랜잭션으로 보장

**Race Condition 대응**
- 좋아요/싫어요: Redis 원자적 연산으로 중복 방지
- 조회수 증가: `@Async`로 비동기 처리하여 경합 최소화

---

## 🚀 개발 환경

### 필수 요구사항

**Language Version**
- Java 17 이상

**DB / Cache**
- MySQL 8.0 이상
- Redis 7.0 이상

**Build Tool**
- Gradle 8.0 이상

### 실행 방법

1. **MySQL 데이터베이스 생성**
```sql
CREATE DATABASE linkup CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. **Redis 실행**
```bash
redis-server
```

3. **애플리케이션 설정**
- `src/main/resources/application.properties`에서 DB, Redis 연결 정보 수정

4. **애플리케이션 실행**
```bash
./gradlew bootRun
# 또는
java -jar build/libs/link-up-0.0.1-SNAPSHOT.jar
```

5. **접속**
- 웹 애플리케이션: `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- 모니터링: `http://localhost:8081/monitoring` (로컬 IP만 접근 가능)

### Docker 실행

```bash
docker build -t link-up .
docker run -p 8081:8081 link-up
```

---

## 📚 문서

### 아키텍처 문서
- 전체 아키텍처: `src/main/resources/templates/board/linkup.puml` (PlantUML)

### 도메인별 설계 문서
- 게시판: `BoardService`, `BoardController`
- 댓글: `CommentService`, `CommentController`
- 노션: `NotionService`, `NotionController`
- 인증: `SecurityConfig`, `CustomOAuth2UserService`

### 심화 주제

**트랜잭션 / 동시성**
- `@Transactional` 전략: Service 레이어 중심
- Redis 원자적 연산으로 좋아요/싫어요 동시성 제어
- 비동기 처리로 조회수 증가 경합 최소화

**캐싱 전략**
- 공지사항: Redis 캐싱 (TTL: 10분)
- 좋아요/싫어요: Write-Through Cache (5분마다 동기화)
- 캐시 무효화: `@CacheEvict`로 즉시 반영

**성능 개선 사례**
- 공지사항 캐싱으로 조회 속도 **77% 향상** (608ms → 137ms)
- 좋아요/싫어요 Redis 캐싱으로 DB 부하 **80% 감소**
- 비동기 조회수 증가로 응답 시간 **50% 단축**
- Throughput **3배 증가** (144.8/sec → 457.7/sec)

---

## 📝 추가 정보

### 주요 의존성
- Spring Boot 3.3.4
- Spring Security 6
- Spring Data JPA
- MyBatis 3.5.16
- Redis (Lettuce)
- Thymeleaf
- Spring Boot Admin
- SpringDoc OpenAPI

### 라이선스
이 프로젝트는 교육 목적으로 제작되었습니다.
