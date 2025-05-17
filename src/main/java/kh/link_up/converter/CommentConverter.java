package kh.link_up.converter;

import kh.link_up.domain.Comment;
import kh.link_up.dto.CommentDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommentConverter implements EntitiyConverter<Comment, CommentDTO> {

    public List<CommentDTO> convertList(List<Comment> commentsList) {
        return commentsList.stream()
                .map(this::convertToDTO)  // 개별 Users 객체를 UsersDTO로 변환
                .collect(Collectors.toList());
    }

    @Override
    public CommentDTO convertToDTO(Comment entity) {
        if (entity == null) {
            return null;
        }

        String cWriterName = null;
        if (entity.getWriter() != null) {
            cWriterName = entity.getWriter().getUNickname();
        }else{
            cWriterName = entity.getSocialUser().getName();
        }

        return CommentDTO.builder()
                .c_idx(entity.getCIdx())  // 댓글 고유 ID
                .c_writer(cWriterName)  // 댓글 작성자 이름
                .c_content(entity.getCContent())  // 댓글 내용
                .c_username(entity.getCUsername())  // 댓글 작성자 이름 (사용자명)
                .c_like(entity.getCLike())  // 댓글 좋아요 수
                .c_upLoad(entity.getCUpload())
                .c_deleted(entity.isCDeleted())
                .c_dislike(entity.getCDislike())
                .c_report(entity.getCReport())
                .socialUser(entity.getSocialUser())
                .build();
    }

    @Override
    public Comment convertToEntity(CommentDTO dto) {
        if (dto == null) {
            return null;
        }

        return Comment.builder()
                .cIdx(dto.getC_idx())  // 댓글 고유 ID
                .cContent(dto.getC_content())  // 댓글 내용
                .cUsername(dto.getC_username())  // 댓글 작성자 이름
                .cLike(dto.getC_like() != null ? dto.getC_like() : 0)  // 댓글 좋아요 수
                .cUpload(dto.getC_upLoad())
                .cDeleted(dto.isC_deleted())
                .cReport(dto.getC_report())
                .cDislike(dto.getC_dislike())
                .socialUser(dto.getSocialUser())
                .build();
    }
}

