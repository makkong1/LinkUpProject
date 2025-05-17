package kh.link_up.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import kh.link_up.domain.SocialUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long c_idx;
    private String c_writer;
    private String c_content;
    private String c_username;
    private Integer c_like;
    private Long b_idx;
    private java.sql.Timestamp c_upLoad;
    private boolean c_deleted = false;
    private int c_dislike;
    private int c_report;
    private SocialUser socialUser;

    //이거는 소셜유저때문에 쓰는거임 그 실제 db컬럼은 아님
    @JsonProperty("uEmail")
    private String uEmail;
}

