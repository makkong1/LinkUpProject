package kh.link_up.service;

import kh.link_up.domain.TargetType;
import kh.link_up.repository.BoardRepository;
import kh.link_up.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeDislikeSyncService {

    private final LikeDislikeCacheService cacheService;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;

    @Scheduled(fixedRate = 300_000)
    public void syncLikeDislikeToDB() {
        log.debug("🟡 [좋아요/싫어요 동기화 시작]");

        syncTarget(TargetType.BOARD);
        syncTarget(TargetType.COMMENT);

        log.debug("✅ [좋아요/싫어요 동기화 완료]");
    }

    private void syncTarget(TargetType targetType) {
        Set<Object> changedIds = cacheService.getChangedIds(targetType);
        log.info("[{}] 캐시 대상 ID: {}", targetType, changedIds);

        if (changedIds == null || changedIds.isEmpty()) {
            log.debug("⚪ [{}] 동기화 대상 없음", targetType);
            return;
        }

        for (Object obj : changedIds) {
            Long id;
            try {
                id = Long.parseLong(obj.toString());
            } catch (NumberFormatException e) {
                continue;
            }

            Long likeCount = cacheService.getLikeCount(targetType, id);
            Long dislikeCount = cacheService.getDislikeCount(targetType, id);

            if (likeCount == 0 && dislikeCount == 0) {
                cacheService.resetCounts(targetType, id);
                continue;
            }

            // 대상별 DB 업데이트
            if (targetType == TargetType.BOARD) {
                boardRepository.updateLikeDislikeCount(id, likeCount.intValue(), dislikeCount.intValue());
            } else if (targetType == TargetType.COMMENT) {
                commentRepository.updateLikeDislikeCount(id, likeCount.intValue(), dislikeCount.intValue());
            }

            log.debug("🟢 [{}] ID={} → +{}/+{}", targetType, id, likeCount, dislikeCount);

            cacheService.resetCounts(targetType, id); // Redis 초기화
        }
    }
}
