package kh.link_up.service;

import kh.link_up.converter.CommentConverter;
import kh.link_up.domain.Board;
import kh.link_up.domain.Comment;
import kh.link_up.domain.SocialUser;
import kh.link_up.domain.Users;
import kh.link_up.dto.CommentDTO;
import kh.link_up.repository.BoardRepository;
import kh.link_up.repository.CommentRepository;
import kh.link_up.repository.SocialUserRepository;
import kh.link_up.repository.UsersRepository;
import kh.link_up.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final UsersRepository usersRepository;
    private final CommentConverter commentConverter;
    private final BoardRepository boardRepository;
    private final SocialUserRepository socialUserRepository;
    private final UserUtil userUtil;

    @Transactional
    public Comment createComment(CommentDTO commentRequestDto, Principal principal, Authentication authentication) {
        log.info("idx : {}", commentRequestDto);

        Board board = boardRepository.findById(commentRequestDto.getB_idx())
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글(idx: " + commentRequestDto.getB_idx() + ")을 찾을 수 없습니다."));

        // 1. 로그인한 사용자 정보 가져오기
        String nicknameOrEmail = userUtil.getUserIdentifier(principal, authentication); // 닉네임 or 이메일
        String displayName = userUtil.getUserNickname(principal); // 보여줄 이름 (닉네임)

        Users writer = usersRepository.findByuNickname(nicknameOrEmail);  // 일반 사용자 기준
        SocialUser socialUser = null;

        if (writer == null) {
            socialUser = socialUserRepository.findByEmail(nicknameOrEmail);
        }

        Comment comment = Comment.builder()
                .writer(writer)
                .socialUser(socialUser)
                .cContent(commentRequestDto.getC_content())
                .cUsername(displayName)  // 유틸에서 가져온 닉네임
                .board(board)
                .build();

        return commentRepository.save(comment);
    }

    public Page<CommentDTO> getCommentsAll(Long id, Pageable pageable) {
        Page<Comment> commentPage = commentRepository.findCommentByBIdx(id, pageable);

        Page<CommentDTO> commentDTOPage = commentPage.map(comment -> {
            CommentDTO commentDTO = commentConverter.convertToDTO(comment);

            String maskedName;
            if (comment.getSocialUser() != null) {
                // 소셜 유저일 경우 소셜 유저의 이름을 마스킹
                maskedName = maskName(comment.getSocialUser().getName());
            } else {
                // 일반 유저일 경우 유저 닉네임을 마스킹
                maskedName = maskName(comment.getWriter().getUNickname());
            }
            commentDTO.setC_writer(maskedName);
            log.info("maskName : {}", maskedName);
            return commentDTO;
        });

        return commentDTOPage;
    }

    //소셜유저 이름이 그대로 보여서 일단 만듬 문제는 이렇게하면 아마 그 일반유저이름도 그대로 안보일듯
    public String maskName(String name) {
        if (name == null || name.length() <= 1) {
            return name;  // 이름이 1자 이하라면 그대로 반환
        }

        // 첫 글자만 남기고 나머지는 '*'로 변경

        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    public Page<CommentDTO> getReportComment(Pageable pageable){
        return commentRepository.findReportComment(pageable).map(commentConverter::convertToDTO);
    }

    public Page<Comment> getFilteredComments(String selectComment, String inputTextComment, Pageable pageable) {
        String selectCommentValue = selectComment.trim();
        String textCommentValue = inputTextComment.trim();
        return switch (selectCommentValue) {
            case "content_comment" -> commentRepository.findByContentContaining(textCommentValue, pageable);
            case "writer_comment" -> commentRepository.findByWriterContaining(textCommentValue, pageable);
            default -> commentRepository.findAll(pageable);
        };
    }

    @Transactional
    public void deleteComment(Long cIdx) {
        // 댓글을 DB에서 조회
        Comment comment = commentRepository.findById(cIdx)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        // 삭제 처리 (c_deleted 값을 1로 설정)
        comment.setCDeleted(true);  // 삭제 처리: c_deleted를 true로 설정

        // 변경된 댓글을 저장 (JPA가 자동으로 업데이트 해줌)
        commentRepository.save(comment);  // 업데이트 쿼리가 실행됨
    }

    //댓글 신고
    public boolean reportComment(Long cIdx) {
        Optional<Comment> optionalComment = commentRepository.findById(cIdx);

        if (optionalComment.isPresent()) {
            Comment comment = optionalComment.get();
            comment.increaseCommentReport(false);  // 신고 수 증가
            commentRepository.save(comment);  // 변경된 댓글 저장
            return true;  // 성공적으로 신고 처리됨
        } else {
            throw new IllegalArgumentException("해당 댓글을 찾을 수 없습니다.");  // 예외 발생
        }
    }

    // 관리자 댓글 삭제
    public boolean deleteCommentForAdmin(Long cIdx) {
        try {
            Comment reportComment = commentRepository.findById(cIdx)
                    .orElseThrow(() -> new NoSuchElementException("Comment with id " + cIdx + " not found"));

            reportComment.setCDeleted(true);
            commentRepository.save(reportComment);
            return true;
        } catch (NoSuchElementException e) {
            // 댓글이 없을 때 발생하는 예외 처리
            log.error("deleteCommentForAdmin 실패 " + e.getMessage());
            return false;
        } catch (Exception e) {
            // 예상치 못한 예외 처리
            log.error("deleteCommentForAdmin 에러발생: " + e.getMessage());
            return false;
        }
    }

    //관리자 댓글 복원
    public boolean resolveCommentForAdmin(Long cIdx) {
        try {
            Comment reportComment = commentRepository.findById(cIdx)
                    .orElseThrow(() -> new NoSuchElementException("Comment with id " + cIdx + " not found"));

            reportComment.setCDeleted(false);
            reportComment.setCReport(0);
            commentRepository.save(reportComment);
            return true;
        } catch (NoSuchElementException e) {
            // 댓글이 없을 때 발생하는 예외 처리
            log.error("resolveCommentForAdmin 실패 " + e.getMessage());
            return false;
        } catch (Exception e) {
            // 예상치 못한 예외 처리
            log.error("resolveCommentForAdmin 에러발생: " + e.getMessage());
            return false;
        }
    }

}
