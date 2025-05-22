package kh.link_up.service;

import kh.link_up.domain.Board;
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

    @Scheduled(fixedRate = 300_000) // 5분마다 실행
    public void syncLikesAndDislikesToDB() {
        log.debug("🟡 [동기화 시작]");

        Set<Object> boardIdObjects = cacheService.getChangedBoardIds();
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

            Board board = boardRepository.findById(boardId).orElse(null);
            if (board == null) {
                cacheService.resetCounts(boardId); // 존재하지 않는 게시글이므로 정리
                continue;
            }

            Long likeCount = cacheService.getLikeCount(boardId);
            Long dislikeCount = cacheService.getDislikeCount(boardId);

            if (likeCount > 0) {
                board.setLikeCount(board.getLikeCount() + likeCount.intValue());
                log.debug("🟢 좋아요 동기화: boardId={} +{}", boardId, likeCount);
            }
            if (dislikeCount > 0) {
                board.setDislikeCount(board.getDislikeCount() + dislikeCount.intValue());
                log.debug("🔴 싫어요 동기화: boardId={} +{}", boardId, dislikeCount);
            }

            boardRepository.save(board);
            cacheService.resetCounts(boardId); // 캐시 삭제
        }

        log.debug("✅ [동기화 완료]");
    }
}
