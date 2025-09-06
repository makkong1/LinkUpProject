package kh.link_up.converter;

import kh.link_up.domain.Comment;
import kh.link_up.dto.CommentDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CommentConverter implements EntitiyConverter<Comment, CommentDTO> {

    @Override
    public CommentDTO convertToDTO(Comment entity) {
        if (entity == null) {
            return null;
        }

        // 작성자 정보: 일반 사용자 or 소셜 사용자
        String writerName = null;
        String uEmail = null;

        if (entity.getWriter() != null) {
            writerName = entity.getWriter().getUNickname();  // 일반 사용자 닉네임
            log.info("일반 댓글작성자이름 : {}", writerName);

        } else if (entity.getSocialUser() != null) {
            writerName = entity.getSocialUser().getName();    // 소셜 사용자 이름
            uEmail = entity.getSocialUser().getEmail();      // 소셜 사용자 이메일
            log.info("소셜 댓글 작성자 이름 : {}", writerName);
            log.info("소셜 댓글 작성자 이메일 : {}", uEmail);

        }

        return CommentDTO.builder()
                .c_idx(entity.getCIdx())
                .c_writer(writerName)
                .c_content(entity.getCContent())
//                .c_username(entity.getCUsername())
                .c_like(entity.getCLike())
                .c_upLoad(entity.getCUpload())
                .c_deleted(entity.isCDeleted())
                .c_dislike(entity.getCDislike())
                .c_report(entity.getCReport())
                .b_idx(entity.getBoard() != null ? entity.getBoard().getBIdx() : null)
                .socialUser(entity.getSocialUser())
                .uEmail(uEmail)
                .build();
    }

    @Override
    public Comment convertToEntity(CommentDTO dto) {
        if (dto == null) {
            return null;
        }

        return Comment.builder()
                .cIdx(dto.getC_idx())
                .cContent(dto.getC_content())
//                .cUsername(dto.getC_username())
                .cLike(dto.getC_like() != null ? dto.getC_like() : 0)
                .cUpload(dto.getC_upLoad())
                .cDeleted(dto.isC_deleted())
                .cDislike(dto.getC_dislike())
                .cReport(dto.getC_report())
                .socialUser(dto.getSocialUser())  // 실제 연관 관계는 서비스단에서 주입
                .build();
    }

    // 리스트 변환
    public List<CommentDTO> convertList(List<Comment> commentsList) {
        return commentsList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
