package kh.link_up.dto;

import kh.link_up.domain.Board;
import kh.link_up.domain.Comment;
import kh.link_up.domain.SocialUser;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class BoardDTO {
    private Long bIdx; // 게시글 고유 ID (b_idx -> bIdx)
    private String bWriter; // 게시글 작성자 ID (b_writer -> bWriter)
    private String category; // 게시글 카테고리 (b_category -> category)
    private String title; // 제목 (b_title -> title)
    private String content; // 내용 (b_content -> content)
    private String password; // 비밀번호 (b_pwd -> password)
    private java.sql.Timestamp uploadTime; // 업로드 시간 (b_upload -> uploadTime)
    private Integer viewCount; // 조회수 (b_view_cnt -> viewCount)
    private Integer likeCount; // 좋아요 수 (b_like -> likeCount)
    private String filePath; // 파일 경로 (b_file_path -> filePath)
    private String isDeleted; // 삭제 여부 (b_is_deleted -> isDeleted)
    private String b_report;
    private List<Comment> comments;
    private List<Board> board;
    private SocialUser socialUser;
    private int dislikeCount;
}
