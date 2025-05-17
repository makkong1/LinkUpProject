package kh.link_up.dto;

import kh.link_up.domain.Board;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@NoArgsConstructor
@Data
public class BoardListDTO implements Serializable {
    private Long bIdx;
    private String title;
    private String category;
    private String writerName;
    private LocalDateTime uploadTime;
    private int viewCount;
    private String isDeleted; // 삭제 여부 (b_is_deleted -> isDeleted)

    public BoardListDTO(Board board) {
        this.bIdx = board.getBIdx();
        this.title = board.getTitle();
        this.category = board.getCategory();
        this.uploadTime = board.getUploadTime().toLocalDateTime();
        this.viewCount = board.getViewCount();
        this.isDeleted = board.getIsDeleted();

        if (board.getWriter() != null) {
            this.writerName = board.getWriter().getUNickname();
        } else if (board.getSocialUser() != null) {
            this.writerName = board.getSocialUser().getName();
        } else {
            this.writerName = "익명 사용자";
        }
    }
}

