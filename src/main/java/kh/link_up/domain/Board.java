package kh.link_up.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "board") // 테이블명 소문자로 일관성 있게 변경
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "b_idx") // 컬럼명 표준화
    private Long bIdx; // 게시글 고유 ID (b_idx -> b_idx)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "b_writer", referencedColumnName = "u_idx", nullable = false) // 외래키 컬럼 표준화
    private Users writer; // 일반사용자 게시글 작성자

    @ManyToOne(fetch = FetchType.LAZY) // 소셜 로그인 사용자를 참조
    @JoinColumn(name = "social_user_id", referencedColumnName = "social_user_id", nullable = true) // 외래키 추가
    private SocialUser socialUser; // 소셜 로그인 사용자

    @Column(name = "b_category")
    private String category; // 게시글 카테고리 (b_category -> b_category)

    @Column(name = "b_title", nullable = false)
    private String title; // 제목 (b_title -> b_title)

    @Column(name = "b_content", nullable = false, columnDefinition = "TEXT")
    private String content; // 내용 (b_content -> b_content)

    @Column(name = "b_pwd")
    private String password; // 비밀번호 (b_pwd -> b_pwd)

    @CreationTimestamp
    @Column(name = "b_upload", updatable = false)
    private java.sql.Timestamp uploadTime; // 업로드 시간 (b_upload -> b_upload)

    @Column(name = "b_view_cnt", nullable = false)
    @Builder.Default
    private Integer viewCount = 0; // 조회수 (b_view_cnt -> b_view_cnt)

    @Column(name = "b_like", nullable = false)
    @Builder.Default
    private Integer likeCount = 0; // 좋아요 수 (b_like -> b_like)

    @Column(name = "b_dislike", nullable = false)
    @Builder.Default
    private Integer dislikeCount = 0; // 좋아요 수 (b_like -> b_like)

    @Column(name = "b_file_path")
    private String filePath; // 파일 경로 (b_file_path -> b_file_path)

    @Column(name = "b_isdeleted") // 삭제 여부 (b_isdeleted -> b_isdeleted)
    private String isDeleted; // 삭제 여부 (b_isdeleted -> isDeleted)

    @Column(name = "b_report") // 신고 여부 (새로 추가된 컬럼)
    @Builder.Default
    private Integer report = 0; // 신고 수 (기본값 0)

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    // 추가된 writerNickname 필드
    @Transient
    private String writerNickname; // 임시 필드, 엔티티에는 저장되지 않음

    public String getFileName() {
        if (filePath == null || filePath.isEmpty()) {
            return "파일 경로가 유효하지 않습니다."; // null이나 빈 문자열일 때 반환할 값
        }

        Path path = Paths.get(filePath);

        if (path.getFileName() == null) {
            return "파일명이 존재하지 않습니다."; // 경로에 파일명이 없을 때
        }

        return path.getFileName().toString(); // 파일명만 반환
    }


    public void incrementViewCount(boolean resetToZero) {
        if (resetToZero) {
            this.viewCount = 0; // 0으로 초기화
        } else {
            this.viewCount += 1; // 1 증가
        }
    }

    public void incrementBoardReport(boolean resetToZero) {
        if (resetToZero) {
            this.report = 0; // 0으로 초기화
        } else {
            this.report += 1; // 1 증가
        }
    }

    // 좋아요 증가 메서드
    public void increaseLikeCount() {
        if (this.likeCount >= 0) {
            this.likeCount++;
        }
    }

    // 싫어요 감소 메서드
    public void decreaseDislikeCount() {
        if (this.dislikeCount >= 0) {
            this.dislikeCount++;
        }
    }

}
