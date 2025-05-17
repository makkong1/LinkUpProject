package kh.link_up.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "social_user")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SocialUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_user_id")
    private Long socialUserId;  // 소셜 사용자 고유 ID

    @Column(name = "provider", nullable = false)
    private String provider;  // 소셜 제공자 (Google, Naver 등)

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;  // 소셜 제공자에서 받은 고유 ID

    @Column(name = "email")
    private String email;  // 이메일 (Google, Naver 공통)

    @Column(name = "name")
    private String name;  // 사용자 이름 (Google, Naver 공통)

    @Column(name = "profile_image_url")
    private String profileImageUrl;  // 프로필 이미지 URL (Google 제공)

    @Column(name = "phone")
    private String phone;  // 전화번호 (Naver 제공)

    @Column(name = "birthyear")
    private String birthyear;  // 출생년도 (Naver 제공)

    @Column(name = "gender")
    private String gender;  // 성별 (Naver 제공)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;  // 생성일

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;  // 업데이트일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")  // Users 테이블과 연관
    private Users user;  // Users 객체와의 관계

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;  // Long 타입의 user_id 필드 추가

    @OneToMany(mappedBy = "socialUser", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude // 순환 참조 방지
    private List<Board> board = new ArrayList<>();

    @OneToMany(mappedBy = "socialUser", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude // 순환 참조 방지
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "socialUser", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude // 순환 참조 방지
    private List<Notion> notions = new ArrayList<>();

}
