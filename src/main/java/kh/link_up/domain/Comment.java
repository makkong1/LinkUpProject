package kh.link_up.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "comment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cIdx; // 댓글 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "c_writer", nullable = false)
    private Users writer; // 댓글 작성자 ID (외래키)

    @ManyToOne(fetch = FetchType.LAZY) // 다대일 관계 설정
    @JoinColumn(name = "b_idx", nullable = false) // 게시글 ID와 연결 (외래키)
    @ToString.Exclude // 순환 참조 방지
    private Board board; // 댓글이 달린 게시글

    @ManyToOne(fetch = FetchType.LAZY) // 소셜 로그인 사용자를 참조
    @JoinColumn(name = "social_user_id", referencedColumnName = "social_user_id", nullable = true) // 외래키 추가
    private SocialUser socialUser; // 소셜 로그인 사용자

    @Column(nullable = false, length = 200)
    private String cContent; // 댓글 내용

    @Column(nullable = false, length = 50)
    private String cUsername; // 댓글 작성자 이름

    @CreationTimestamp
    private java.sql.Timestamp cUpload; // 댓글 작성일

    @Column(nullable = false)
    @Builder.Default
    private Integer cLike = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer cDislike = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean cDeleted = false;

    @Column(nullable = false)
    @Builder.Default
    private int cReport = 0;

    public void increaseCommentReport(boolean resetToZero) {
        if (resetToZero) {
            cReport = 0;
        } else {
            cReport += 1;
        }
    }

}
