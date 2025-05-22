package kh.link_up.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeDislikeCacheService {

    private static final String LIKE_KEY_PREFIX = "board:like:";
    private static final String DISLIKE_KEY_PREFIX = "board:dislike:";
    private static final String CHANGED_SET_KEY = "board:changed";

    private final RedisTemplate<String, Object> redisTemplate;

    // 좋아요 증가
    public void increaseLikeCount(Long boardId) {
        String key = LIKE_KEY_PREFIX + boardId;
        redisTemplate.opsForValue().increment(key, 1);
        redisTemplate.opsForSet().add(CHANGED_SET_KEY, boardId.toString()); // 변경된 게시글 기록
    }

    // 싫어요 증가
    public void increaseDislikeCount(Long boardId) {
        String key = DISLIKE_KEY_PREFIX + boardId;
        redisTemplate.opsForValue().increment(key, 1);
        redisTemplate.opsForSet().add(CHANGED_SET_KEY, boardId.toString()); // 변경된 게시글 기록
    }

    public Long getLikeCount(Long boardId) {
        String key = LIKE_KEY_PREFIX + boardId;
        Object val = redisTemplate.opsForValue().get(key);
        return val == null ? 0L : Long.parseLong(val.toString());
    }

    public Long getDislikeCount(Long boardId) {
        String key = DISLIKE_KEY_PREFIX + boardId;
        Object val = redisTemplate.opsForValue().get(key);
        return val == null ? 0L : Long.parseLong(val.toString());
    }

    public void resetCounts(Long boardId) {
        redisTemplate.delete(LIKE_KEY_PREFIX + boardId);
        redisTemplate.delete(DISLIKE_KEY_PREFIX + boardId);
        redisTemplate.opsForSet().remove(CHANGED_SET_KEY, boardId.toString()); // Set에서 제거
    }

    public Set<Object> getChangedBoardIds() {
        return redisTemplate.opsForSet().members(CHANGED_SET_KEY);
    }
}
