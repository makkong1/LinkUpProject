package kh.link_up.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "u_idx")
    private Long uIdx;

    @Column(name = "u_username", nullable = false, length = 50)
    private String uUsername; // 사용자 이름

    @Column(name = "u_id", nullable = false, unique = true, length = 50)
    private String id; // 로그인 ID

    @Column(name = "u_pwd", nullable = false, length = 255)
    private String password; // 비밀번호

    @Column(name = "u_nickname", nullable = false, length = 50)
    private String uNickname; // 닉네임

    @Column(name = "u_email", nullable = false, length = 100)
    private String uEmail; // 이메일

    @Column(name = "u_telephone", length = 20)
    private String uTelephone; // 전화번호 (선택 사항)

    @Column(name = "u_birthday")
    private java.sql.Date uBirthday; // 생년월일 (선택 사항), 년월일만 생성, dates 이걸로 타임리프에서표현

    @CreationTimestamp
    @Column(name = "u_created_at", updatable = false)
    private LocalDateTime uCreatedAt; // 계정 생성일, 시간까지 다 생성함 temporals 이걸로 타임리프에서표현

    @Column(name = "u_block", length = 255)
    private String uBlockReason; // 차단 사유

    @Column(name = "u_role", nullable = false, length = 6, columnDefinition = "char(6) default 'USER'")
    private String uRole; // 역할 (admin, user)

    @Column(name = "failed_login_attempts", nullable = false, columnDefinition = "int default 0")
    private int failedLoginAttempts; // 로그인 실패 횟수

    @Column(name = "account_locked", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean accountLocked; // 계정 잠금 여부

    @OneToMany(mappedBy = "writer", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude // 순환 참조 방지
    @Builder.Default
    private List<Board> board = new ArrayList<>();

    @OneToMany(mappedBy = "writer", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude // 순환 참조 방지
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "writer", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude // 순환 참조 방지
    @Builder.Default
    private List<Notion> notions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude // 순환 참조 방지
    @Builder.Default
    private List<SocialUser> socialUser = new ArrayList<>();

    // 소셜 유저인지 일반 유저인지 판단할 수 있는 메서드
    public boolean isSocialUser() {
        return !socialUser.isEmpty(); // 소셜 유저가 존재하면 true
    }

    // 로그인 실패 횟수 증가
    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

}
