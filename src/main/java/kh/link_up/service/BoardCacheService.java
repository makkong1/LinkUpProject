package kh.link_up.service;

import kh.link_up.domain.Board;
import kh.link_up.dto.BoardListDTO;
import kh.link_up.dto.BoardListDTOWrapper;
import kh.link_up.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
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

    //공지사항 캐시 등록
    @Cacheable(value = "noticeBoards", key = "'list'")
    public BoardListDTOWrapper getNoticeBoard() {
        log.info("DB에서 조회 후 캐시합니다.");

        List<Board> boards = boardRepository.findByCategory("NOTICE");

        // BoardListDTO 리스트를 BoardListDTOWrapper로 감싸서 반환
        List<BoardListDTO> boardListDTOs = boards.stream()
                .map(BoardListDTO::new)
                .collect(Collectors.toList());
//
//        boardListDTOs.forEach(dto -> log.debug("boardListDTO: {}", dto));
        log.debug("공지사항 캐시 : {}", boardListDTOs);
        // BoardListDTOWrapper를 생성하여 반환
        return new BoardListDTOWrapper(boardListDTOs);
    }

    //공지사항 캐시 삭제
    @CacheEvict(value = "noticeBoards", key = "'list'")
    public void clearNoticeBoardCache() {
        log.info("공지사항 캐시 초기화");
    }

//    @CacheEvict는 getNoticeBoard()를 자동으로 다시 캐싱해주지 않기 때문에
//    직접 redisTemplate으로 넣어야 합니다.
//    따라서 refreshNoticeBoardCache()는 "캐시 비우고, 다시 저장하는" 역할을 동시에 하도록 구성된 겁니다.
//    공지사항 캐시 업데이트
//    @CacheEvict(value = "noticeBoards", key = "'list'")
//    public void refreshNoticeBoardCache() {
//        log.info("공지사항 캐시 초기화 후 재등록");
//
//        // 캐시 초기화 후 DB에서 다시 조회하여 캐시 강제 저장
//        BoardListDTOWrapper wrapper = getNoticeBoard();
//        redisTemplate.opsForValue().set("noticeBoards::list", wrapper); // 수동 저장
//    }

    // 공지사항 캐시 업데이트
    @CachePut(value = "noticeBoards", key = "'list'")
    public BoardListDTOWrapper refreshNoticeBoardCache() {
        log.info("공지사항 새로 등록 후 캐시 강제 갱신");

        List<Board> boards = boardRepository.findByCategory("NOTICE");

        List<BoardListDTO> boardListDTOs = boards.stream()
                .map(BoardListDTO::new)
                .collect(Collectors.toList());

        log.debug("공지사항 재등록 : {}",boardListDTOs);

        return new BoardListDTOWrapper(boardListDTOs);
    }

}
