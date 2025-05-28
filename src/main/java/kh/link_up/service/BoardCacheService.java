package kh.link_up.service;

import kh.link_up.domain.Board;
import kh.link_up.dto.BoardListDTO;
import kh.link_up.dto.BoardListDTOWrapper;
import kh.link_up.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
public class BoardCacheService {

    private final BoardRepository boardRepository;
    private static final String LIKE_PREFIX = "board:like:";
    private static final String DISLIKE_PREFIX = "board:dislike:";
    private final RedisTemplate<String, Object> redisTemplate;

    @Cacheable(value = "noticeBoards", key = "'list'")
    public BoardListDTOWrapper getNoticeBoard() {
        log.info("DB에서 조회 후 캐시합니다.");

        List<Board> boards = boardRepository.findByCategory("NOTICE");

        // BoardListDTO 리스트를 BoardListDTOWrapper로 감싸서 반환
        List<BoardListDTO> boardListDTOs = boards.stream()
                .map(BoardListDTO::new)
                .collect(Collectors.toList());
        boardListDTOs.forEach(dto -> log.debug("boardListDTO: {}", dto));
        // BoardListDTOWrapper를 생성하여 반환
        return new BoardListDTOWrapper(boardListDTOs);
    }

    @CacheEvict(value = "noticeBoards", key = "'list'")
    public void clearNoticeBoardCache() {
        log.info("공지사항 캐시 초기화");
    }

}
