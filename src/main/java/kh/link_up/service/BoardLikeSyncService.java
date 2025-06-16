package kh.link_up.service;

import kh.link_up.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardLikeSyncService {

    private final LikeDislikeCacheService cacheService;
    private final BoardRepository boardRepository;

    @Scheduled(fixedRate = 300_000)
    public void syncLikesAndDislikesToDB() {
        log.debug("🟡 [동기화 시작]");

        Set<Object> boardIdObjects = cacheService.getChangedBoardIds();
        log.info("보드 좋아요,싫어요 캐쉬 id : {}", boardIdObjects);

        if (boardIdObjects == null || boardIdObjects.isEmpty()) {
            log.debug("⚪ 동기화 대상 없음");
            return;
        }

        for (Object obj : boardIdObjects) {
            Long boardId;
            try {
                boardId = Long.parseLong(obj.toString());
            } catch (NumberFormatException e) {
                continue;
            }

            Long likeCount = cacheService.getLikeCount(boardId);
            Long dislikeCount = cacheService.getDislikeCount(boardId);

            if (likeCount == 0 && dislikeCount == 0) {
                cacheService.resetCounts(boardId);
                continue;
            }

            boardRepository.updateLikeDislikeCount(boardId, likeCount.intValue(), dislikeCount.intValue());
            log.debug("🟢 좋아요/싫어요 동기화: boardId={} +{}/+{}", boardId, likeCount, dislikeCount);

            cacheService.resetCounts(boardId);
        }

        log.debug("✅ [동기화 완료]");
    }

}
