package kh.link_up.dto;

import kh.link_up.domain.Board;
import kh.link_up.domain.Comment;
import kh.link_up.domain.Notion;
import kh.link_up.domain.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Component
public class SocialUserDTO {

    private Long socialUserId;  // 소셜 사용자 고유 ID
    private String provider;  // 소셜 제공자 (Google, Naver 등)
    private String providerUserId;  // 소셜 제공자에서 받은 고유 ID
    private String email;  // 이메일 (Google, Naver 공통)
    private String name;  // 사용자 이름 (Google, Naver 공통)
    private String profileImageUrl;  // 프로필 이미지 URL (Google 제공)
    private String phone;  // 전화번호 (Naver 제공)
    private String birthyear;  // 출생년도 (Naver 제공)
    private String gender;  // 성별 (Naver 제공)
    private Users users;
    private Long userId;  // Users 테이블의 user_id (연관된 사용자 ID)
    private LocalDateTime createdAt;  // 생성일
    private LocalDateTime updatedAt;  // 업데이트일
    private List<Board> board;
    private List<Comment> comments;
    private List<Notion> notion;
}
