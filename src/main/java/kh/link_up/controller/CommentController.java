package kh.link_up.controller;

import kh.link_up.converter.CommentConverter;
import kh.link_up.domain.Comment;
import kh.link_up.dto.CommentDTO;
import kh.link_up.service.CommentNotificationSubscriber;
import kh.link_up.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@RequestMapping("/users")
@Slf4j
public class CommentController {

    private final CommentService commentService;
    private final RedisTemplate<String, String> redisTemplate;
    private final CommentNotificationSubscriber notificationSubscriber;
    private final CommentConverter commentConverter;
    // 클라이언트와의 연결을 관리할 Map
    private final Map<String, SseEmitter> clientConnections = new ConcurrentHashMap<>();

    @PostMapping("/board/comments")
    public ResponseEntity<?> createComment(@RequestBody CommentDTO commentDto) {
        log.info("댓글작성 들어옴");
        log.info("createComment : {}", commentDto);

        // 댓글 생성
        Comment comment = commentService.createComment(commentDto);
        log.info("comment info :{}", comment);

        // 댓글 작성 완료 후 Redis로 알림 메시지 발행
        String boardTitle = comment.getBoard().getTitle();
        String notificationMessage = String.format(
                "%s : %s 게시글에 댓글이 달렸습니다. <a href='/board/%d'>게시글로 이동</a>",
                comment.getBoard().getWriter().getId(),
                boardTitle,
                comment.getBoard().getBIdx()
        );

        log.debug("Redis로 메시지 발송함: {}", notificationMessage);
        redisTemplate.convertAndSend("comment_notifications", notificationMessage);  // Redis로 메시지 발행

        CommentDTO commentDTO = commentConverter.convertToDTO(comment);
        log.info("commentDto : {}", commentDTO);
        return ResponseEntity.ok(commentDTO);
    }

    // 클라이언트에서 SSE 연결 요청 시 호출
    @GetMapping(value= "/notifications/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getNotifications(@PathVariable String userId) {
        log.debug("userId : {}", userId);
        // 새로운 연결을 위한 SseEmitter 생성
        SseEmitter emitter = new SseEmitter(60 * 1000L);

        // 클라이언트 연결 관리
        notificationSubscriber.addClientConnection(userId, emitter);
        log.debug("현재 clientConnections 상태: {}", notificationSubscriber.getClientConnections());

        // 연결 종료 시 클라이언트 연결 제거
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

    // 댓글 삭제
    @PostMapping("/board/comments/{cIdx}")
    public ResponseEntity<?> deleteComment(@PathVariable Long cIdx) {
        log.info("delete comment cidx : {}", cIdx);
        try {
            commentService.deleteComment(cIdx);
            return ResponseEntity.ok().build();
        }catch (Exception e) {
            log.error("삭제중 에러발생 : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("댓글 삭제에 실패했습니다.");
        }
    };

    //댓글 신고
    @PostMapping("/board/comment/{cIdx}/report")
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());  // 댓글 찾지 못했을 때 예외 처리
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("신고 처리 중 오류가 발생했습니다.");
        }
    }
}
