package kh.link_up.service;

import kh.link_up.domain.TargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class LikeDislikeCacheService {
//
//    private static final String LIKE_KEY_PREFIX = "board:like:";
//    private static final String DISLIKE_KEY_PREFIX = "board:dislike:";
//    private static final String CHANGED_SET_KEY = "board:changed";
//
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    // 좋아요 증가
//    public void increaseLikeCount(Long boardId) {
//        String key = LIKE_KEY_PREFIX + boardId;
//        redisTemplate.opsForValue().increment(key, 1);
//        redisTemplate.opsForSet().add(CHANGED_SET_KEY, boardId.toString()); // 변경된 게시글 기록
//    }
//
//    // 싫어요 증가
//    public void increaseDislikeCount(Long boardId) {
//        String key = DISLIKE_KEY_PREFIX + boardId;
//        redisTemplate.opsForValue().increment(key, 1);
//        redisTemplate.opsForSet().add(CHANGED_SET_KEY, boardId.toString()); // 변경된 게시글 기록
//    }
//
//    public Long getLikeCount(Long boardId) {
//        String key = LIKE_KEY_PREFIX + boardId;
//        Object val = redisTemplate.opsForValue().get(key);
//        return val == null ? 0L : Long.parseLong(val.toString());
//    }
//
//    public Long getDislikeCount(Long boardId) {
//        String key = DISLIKE_KEY_PREFIX + boardId;
//        Object val = redisTemplate.opsForValue().get(key);
//        return val == null ? 0L : Long.parseLong(val.toString());
//    }
//
//    public void resetCounts(Long boardId) {
//        redisTemplate.delete(LIKE_KEY_PREFIX + boardId);
//        redisTemplate.delete(DISLIKE_KEY_PREFIX + boardId);
//        redisTemplate.opsForSet().remove(CHANGED_SET_KEY, boardId.toString()); // Set에서 제거
//    }
//
//    public Set<Object> getChangedBoardIds() {
//        return redisTemplate.opsForSet().members(CHANGED_SET_KEY);
//    }
@Service
@RequiredArgsConstructor
@Slf4j
public class LikeDislikeCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /** Redis Key 생성: {type}:{like|dislike}:{id} */
    private String getKey(TargetType type, String action, Long id) {
        return type.name().toLowerCase() + ":" + action.toLowerCase() + ":" + id;
    }

    /** 좋아요 증가 */
    public void increaseLike(TargetType type, Long id) {
        String key = getKey(type, "like", id);
        redisTemplate.opsForValue().increment(key, 1);
        redisTemplate.opsForSet().add(getChangedSetKey(type), id.toString());
    }

    /** 싫어요 증가 */
    public void increaseDislike(TargetType type, Long id) {
        String key = getKey(type, "dislike", id);
        redisTemplate.opsForValue().increment(key, 1);
        redisTemplate.opsForSet().add(getChangedSetKey(type), id.toString());
    }

    /** 좋아요 수 조회 */
    public Long getLikeCount(TargetType type, Long id) {
        String key = getKey(type, "like", id);
        Object val = redisTemplate.opsForValue().get(key);
        return val == null ? 0L : Long.parseLong(val.toString());
    }

    /** 싫어요 수 조회 */
    public Long getDislikeCount(TargetType type, Long id) {
        String key = getKey(type, "dislike", id);
        Object val = redisTemplate.opsForValue().get(key);
        return val == null ? 0L : Long.parseLong(val.toString());
    }

    /** 해당 대상의 캐시 초기화 (동기화 완료 시) */
    public void resetCounts(TargetType type, Long id) {
        redisTemplate.delete(getKey(type, "like", id));
        redisTemplate.delete(getKey(type, "dislike", id));
        redisTemplate.opsForSet().remove(getChangedSetKey(type), id.toString());
    }

    /** Redis에 변경된 대상 ID를 저장하는 Set 키 생성 (예: changed:board, changed:comment) */
    private String getChangedSetKey(TargetType type) {
        return "changed:" + type.name().toLowerCase();  // e.g., changed:board
    }

    /** Redis에서 좋아요/싫어요 수가 변경된 대상 ID 목록 반환 */
    public Set<Object> getChangedIds(TargetType type) {
        return redisTemplate.opsForSet().members(getChangedSetKey(type));
    }
}
