package kh.link_up.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notion") // 테이블명을 소문자로 일관성 있게 변경
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "n_idx")
    private Long nidX; // 노션 고유 ID (n_idx -> id)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "n_writer", nullable = false) // 'n_writer'는 'users' 테이블의 'u_idx'와 매핑됨
    private Users writer; // 작성자 (User 테이블과의 연관 관계)

    @Column(name = "n_title", nullable = false, length = 255)
    private String nTitle; // 제목 (n_title -> title)

    @Column(name = "n_content", nullable = false, columnDefinition = "TEXT")
    private String nContent; // 내용 (n_content -> content)

    @CreationTimestamp
    @Column(name = "n_upload", updatable = false)
    private LocalDateTime nUploadTime; // 업로드 시간 (n_upload -> uploadTime)

    @Column(name = "n_view_cnt", nullable = false)
    @Builder.Default
    private int nViewCount = 0; // 조회수 (n_view_cnt -> viewCount)

    @Column(name = "n_like", nullable = false)
    @Builder.Default
    private int nLikeCount = 0; // 좋아요 수 (n_like -> likeCount)

    @Column(name = "n_filepath", nullable = true)
    private String nFilePath;

    @ManyToOne(fetch = FetchType.LAZY) // 소셜 로그인 사용자를 참조
    @JoinColumn(name = "social_user_id", referencedColumnName = "social_user_id", nullable = true) // 외래키 추가
    private SocialUser socialUser; // 소셜 로그인 사용자
}
