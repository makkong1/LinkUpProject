package kh.link_up.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kh.link_up.converter.CommentConverter;
import kh.link_up.domain.Comment;
import kh.link_up.domain.TargetType;
import kh.link_up.dto.CommentDTO;
import kh.link_up.service.CommentNotificationSubscriber;
import kh.link_up.service.CommentService;
import kh.link_up.util.LikeDislikeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@RequestMapping("/users")
@Slf4j
@Tag(name = "Comment", description = "댓글 관련 API")
public class CommentController {

    private final CommentService commentService;
    private final RedisTemplate<String, String> redisTemplate;
    private final LikeDislikeUtil likeDislikeUtil;
    private final CommentNotificationSubscriber notificationSubscriber;
    private final CommentConverter commentConverter;
    private final Map<String, SseEmitter> clientConnections = new ConcurrentHashMap<>();

    @Operation(summary = "댓글 작성", description = "댓글을 작성하고 작성된 댓글 정보를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글 작성 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류로 댓글 작성 실패")
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUB_ADMIN')")
    @PostMapping("/board/comments")
    public ResponseEntity<?> createComment(@RequestBody CommentDTO commentDto,  Principal principal,
                                           Authentication authentication) {
        log.info("댓글작성 들어옴");
        log.info("createComment : {}", commentDto);

        Comment comment = commentService.createComment(commentDto, principal, authentication);
        log.info("comment info :{}", comment);

        String boardTitle = comment.getBoard().getTitle();
        String notificationMessage = String.format(
                "%s : %s 게시글에 댓글이 달렸습니다. <a href='/board/%d'>게시글로 이동</a>",
                comment.getBoard().getWriter().getId(),
                boardTitle,
                comment.getBoard().getBIdx()
        );

        // Redis Pub/Sub으로 발행 (comment_notifications 채널)
        log.debug("Redis로 메시지 발송함: {}", notificationMessage);
        redisTemplate.convertAndSend("comment_notifications", notificationMessage);

        CommentDTO commentDTO = commentConverter.convertToDTO(comment);
        log.info("commentDto : {}", commentDTO);
        return ResponseEntity.ok(commentDTO);
    }

    @Operation(summary = "SSE 알림 연결", description = "사용자 ID로 SSE 연결을 생성하여 실시간 알림을 받습니다.")
    @GetMapping(value= "/notifications/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getNotifications(@PathVariable String userId) {
        log.debug("userId : {}", userId);
        SseEmitter emitter = new SseEmitter(60 * 1000L);

        notificationSubscriber.addClientConnection(userId, emitter);
        log.debug("현재 clientConnections 상태: {}", notificationSubscriber.getClientConnections());

        emitter.onCompletion(() -> {
            notificationSubscriber.removeClientConnection(userId);
            log.debug("SseEmitter 연결 완료: userId = {}", userId);
        });
        emitter.onTimeout(() -> {
            notificationSubscriber.removeClientConnection(userId);
            log.debug("SseEmitter 연결 타임아웃: userId = {}", userId);
        });

        return emitter;
    }

    @Operation(summary = "댓글 삭제", description = "댓글 ID로 댓글을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류로 댓글 삭제 실패")
    })
    @PostMapping("/board/comments/{cIdx}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUB_ADMIN')")
    public ResponseEntity<?> deleteComment(@PathVariable Long cIdx) {
        log.info("delete comment cidx : {}", cIdx);
        try {
            commentService.deleteComment(cIdx);
            return ResponseEntity.ok().build();
        }catch (Exception e) {
            log.error("삭제중 에러발생 : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("댓글 삭제에 실패했습니다.");
        }
    }

    @Operation(summary = "댓글 신고", description = "댓글 ID로 댓글을 신고합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "신고 성공"),
            @ApiResponse(responseCode = "404", description = "신고할 댓글을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류로 신고 실패")
    })
    @PostMapping("/board/comment/{cIdx}/report")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUB_ADMIN')")
    public ResponseEntity<?> reportComment(@PathVariable Long cIdx) {
        log.info("신고할 댓글 ID : {}", cIdx);

        try {
            boolean reportComment = commentService.reportComment(cIdx);

            if (reportComment) {
                return ResponseEntity.ok("신고 성공");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("신고 처리 중 오류 발생");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("신고 처리 중 오류가 발생했습니다.");
        }
    }

    // 댓글 좋아요
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUB_ADMIN')")
    @Operation(summary = "댓글 좋아요 카운트", description = "댓글 ID로 댓글 좋아요가 증가합니다.")
    @PostMapping("/comment/{commentId}/like")
    public ResponseEntity<Map<String, Long>> increaseCommentLike(@PathVariable Long commentId) {
        return likeDislikeUtil.likeDislikeprocess(TargetType.COMMENT, commentId, true);
    }

    // 댓글 싫어요
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUB_ADMIN')")
    @Operation(summary = "댓글 싫어요 카운트", description = "댓글 ID로 댓글 싫어요가 증가합니다.")
    @PostMapping("/comment/{commentId}/dislike")
    public ResponseEntity<Map<String, Long>> increaseCommentDislike(@PathVariable Long commentId) {
        return likeDislikeUtil.likeDislikeprocess(TargetType.COMMENT, commentId, false);
    }

}
