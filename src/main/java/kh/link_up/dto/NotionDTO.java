package kh.link_up.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotionDTO {
    private Long n_idx;      // 페이지 고유 ID
    private String n_writer; // 작성자 닉네임 (String으로 수정)
    private String n_title;  // 페이지 제목
    private String n_content;// 페이지 내용
    private Integer n_view_cnt; // 페이지 조회수
    private Integer n_like; // 페이지 좋아요 수
    private LocalDateTime n_upload;  // 이 부분을 추가하세요
    private String n_filepath;
}
