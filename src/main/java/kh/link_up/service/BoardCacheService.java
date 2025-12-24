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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = { @Autowired })
@Slf4j
public class BoardCacheService {

    private final BoardRepository boardRepository;

    // 공지사항 캐시 등록
    @Cacheable(value = "noticeBoards", key = "'list'")
    public BoardListDTOWrapper getNoticeBoard() {
        log.info("DB에서 조회 후 캐시합니다.");

        List<Board> boards = boardRepository.findByCategoryOrderByUploadTimeDesc("NOTICE");

        // BoardListDTO 리스트를 BoardListDTOWrapper로 감싸서 반환
        List<BoardListDTO> boardListDTOs = boards.stream()
                .map(BoardListDTO::new)
                .collect(Collectors.toList());
        //
        // boardListDTOs.forEach(dto -> log.debug("boardListDTO: {}", dto));
        log.debug("공지사항 캐시 : {}", boardListDTOs);
        // BoardListDTOWrapper를 생성하여 반환
        return new BoardListDTOWrapper(boardListDTOs);
    }

    // 공지사항 캐시 삭제
    @CacheEvict(value = "noticeBoards", key = "'list'")
    public void clearNoticeBoardCache() {
        log.info("공지사항 캐시 초기화");
    }

    // 공지사항 캐시 업데이트
    @CachePut(value = "noticeBoards", key = "'list'")
    public BoardListDTOWrapper refreshNoticeBoardCache() {
        log.info("공지사항 새로 등록 후 캐시 강제 갱신");

        List<Board> boards = boardRepository.findByCategoryOrderByUploadTimeDesc("NOTICE");

        List<BoardListDTO> boardListDTOs = boards.stream()
                .map(BoardListDTO::new)
                .collect(Collectors.toList());

        log.debug("공지사항 재등록 : {}", boardListDTOs);

        return new BoardListDTOWrapper(boardListDTOs);
    }

}
