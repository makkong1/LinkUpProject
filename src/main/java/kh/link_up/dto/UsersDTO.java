package kh.link_up.dto;

import jakarta.validation.constraints.*;
import kh.link_up.domain.Board;
import kh.link_up.domain.Comment;
import kh.link_up.domain.Notion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Component
public class UsersDTO {

        private Long uIdx;

        @NotBlank(message = "사용자명은 필수 입력 값입니다.")
        private String uUsername;

        @NotBlank(message = "아이디는 필수 입력 값입니다.")
        private String id;

        @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,15}$", message = "비밀번호는 8~15자리이며, 영문(대소문자 구분없음), 숫자, 특수문자 중 2가지 이상의 조합이어야 합니다.")
        private String pwd;

        @NotBlank(message = "닉네임은 필수 입력 값입니다.")
        @Size(max = 8, message = "닉네임은 최대 8자까지 입력 가능합니다.")
        private String uNickname;

        @Email(message = "유효한 이메일 주소여야 합니다.")
        private String uEmail;

        @NotBlank(message = "전화번호는 필수 입력 값입니다.")
        @Pattern(regexp = "^\\d{3}\\d{4}\\d{4}$", message = "'-'은 빼고 입력하세요. 예: 01012345678")
        private String uTelephone;

        @NotNull(message = "생년월일은 필수 입력 값입니다.")
        private Date uBirthday;

        private LocalDateTime uCreatedAt;
        private String uBlock;
        private String uRole;

        private int loginFailCount; // 로그인 실패 횟수
        private boolean isAccountLocked; // 계정 잠금 상태

        private List<Board> boards;
        private List<Comment> comments;
        private List<Notion> notions;

}
