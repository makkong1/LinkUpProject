package kh.link_up.util;

import kh.link_up.domain.Board;
import kh.link_up.domain.Comment;
import kh.link_up.domain.TargetType;
import kh.link_up.repository.BoardRepository;
import kh.link_up.repository.CommentRepository;
import kh.link_up.service.LikeDislikeCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LikeDislikeUtil {

    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;
    private final LikeDislikeCacheService cacheService;

    public ResponseEntity<Map<String, Long>> likeDislikeprocess(TargetType type, Long id, boolean isLike) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 ID입니다.");
        }

        try {
            // Redis 좋아요/싫어요 수 증가
            if (isLike) {
                cacheService.increaseLike(type, id);
            } else {
                cacheService.increaseDislike(type, id);
            }

            long dbCount = 0L;

            if (type == TargetType.BOARD) {
                Board board = boardRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
                dbCount = isLike ? board.getLikeCount() : board.getDislikeCount();
            } else if (type == TargetType.COMMENT) {
                Comment comment = commentRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
                dbCount = isLike ? comment.getCLike() : comment.getCDislike();
            } else {
                throw new IllegalArgumentException("지원하지 않는 타입입니다.");
            }

            long redisCount = isLike
                    ? cacheService.getLikeCount(type, id)
                    : cacheService.getDislikeCount(type, id);

            String key = isLike ? "likeCount" : "dislikeCount";
            Map<String, Long> result = Map.of(key, dbCount + redisCount);
            log.debug("{} 응답 : {}", key.toUpperCase(), result);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("좋아요/싫어요 처리 중 오류 발생", e);
            throw new RuntimeException("좋아요/싫어요 처리 중 오류가 발생했습니다.");
        }
    }
}
