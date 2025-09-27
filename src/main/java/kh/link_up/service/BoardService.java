package kh.link_up.service;

import kh.link_up.converter.BoardConverter;
import kh.link_up.domain.Board;
import kh.link_up.dto.BoardDTO;
import kh.link_up.dto.BoardListDTO;
import kh.link_up.dto.BoardListDTOWrapper;
import kh.link_up.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardConverter boardConverter;
    private final BoardCacheService boardCacheService;

    // 전체 게시물을 페이징 처리하여 가져오는 메서드 (관리자 전용)
    public Page<Board> getAllPagesBoards(Pageable pageable) {
        return boardRepository.findAll(pageable);
    }

    public Page<BoardListDTO> getAllPagesBoardsForUsers(String selectValue, String text, Pageable pageable) {
        // 공지사항(NOTICE) 게시글을 먼저 가져옵니다.
        BoardListDTOWrapper noticeBoardWrapper = boardCacheService.getNoticeBoard();  // 이제 BoardListDTOWrapper 객체를 반환
        List<BoardListDTO> noticeBoards = noticeBoardWrapper.getBoardListDTO();  // BoardListDTO 리스트 가져오기
        log.debug("캐시 게시글 : {}", noticeBoards);

        // 검색 조건을 확인하여 필터링된 게시글을 가져옵니다.
        Page<Board> filteredBoards;
        if ((selectValue != null && !selectValue.equals("all")) || (text != null && !text.isEmpty())) {
            filteredBoards = getFilteredBoardsForUser(selectValue, text, pageable);
        } else {
            // 'INQUIRY'(문의) 카테고리를 제외한 나머지 게시글을 페이징 처리하여 가져옵니다.
            filteredBoards = boardRepository.findByCategory("GENERAL", pageable);
        }

        // 나머지 게시글을 BoardListDTO로 변환
        List<BoardListDTO> allBoards = new ArrayList<>(noticeBoards);  // 공지사항은 이미 BoardListDTO
        allBoards.addAll(filteredBoards.getContent().stream().map(BoardListDTO::new).toList());  // 나머지 게시글도 DTO로 변환

        // 합쳐진 게시글 리스트를 다시 Page 객체로 변환
        return new PageImpl<>(allBoards, pageable, filteredBoards.getTotalElements() + noticeBoards.size());
    }

    // 게시글  조회 메서드
    @Transactional(readOnly = true)
    public Optional<BoardDTO> getBoardById(Long id) {
        return boardRepository.findById(id)
                .map(board -> boardConverter.convertToDTO(board));
    }

    // 비동기로 게시글 조회수 증가 처리
    @Async
    @Transactional
    public void increaseViewCount(Long id) {
        boardRepository.incrementViewCount(id);
    }

    //게시글 삭제
    public void deleteBoard(Long id) {
        boardRepository.findById(id).ifPresent(board -> {
            // 공지사항이면 캐시 삭제
            //equalsIgnoreCase : str1과 str2의 내용이 대소문자 무시하고 같으면 true 반환
            if ("NOTICE".equalsIgnoreCase(board.getCategory())) {
                boardCacheService.clearNoticeBoardCache();
                log.debug("공지사항 삭제됨 - 캐시 무효화 수행");
            }
        });

        // 게시글 삭제
        boardRepository.deleteById(id);
    }

    // 게시글 신고
    @Transactional
    public boolean reportBoard(Long id) {
        Optional<Board> optionalBoard = boardRepository.findById(id);

        if (optionalBoard.isPresent()) {
            Board board = optionalBoard.get();
            board.setIsDeleted("R"); // 신고된 게시글은 R로 상태 변경
            board.incrementBoardReport(false); // 신고 횟수 증가
            boardRepository.save(board); // 변경된 게시글 저장
            return true; // 신고 처리 성공
        }

        return false; // 해당 게시글이 존재하지 않을 경우 신고 처리 실패
    }

    // 게시글 검색 조건(관리자용) / admincontroller에서 쓰이는 메서드
    public Page<Board> getFilteredBoards(String selectValue, String text, Pageable pageable) {
        selectValue = selectValue.trim();
        text = text.trim();

        return switch (selectValue) {
            case "title" -> boardRepository.searchByTitle(text, pageable); // 제목 검색 + 페이징

            case "writer" -> boardRepository.searchByWriter(text, pageable); // 작성자 검색 + 페이징

            case "content" -> boardRepository.searchByContent(text, pageable); // 내용 검색 + 페이징

            default -> boardRepository.findAll(pageable);
        };
    }

//    게시글 페이징 가져오기 (유저용)
//    public Page<Board> getFilteredBoardsForUser(String selectValue, String text, Pageable pageable) {
//        selectValue = selectValue.trim();
//        text = text.trim();
//
//        // 검색 조건을 한 메소드에서 처리
//        return boardRepository.searchByCriteria(selectValue, text, "INQUIRY", pageable);
//    }

//  게시글 페이징 가져오기 (유저용) / 위에서 쓰이는 메서드
    public Page<Board> getFilteredBoardsForUser(String selectValue, String text, Pageable pageable) {
        selectValue = selectValue.trim();
        text = text.trim();

        return switch (selectValue) {
            case "title" -> boardRepository.searchByTitleForUsers(text, "INQUIRY", pageable);
            case "writer" -> boardRepository.searchByWriterForUsers(text, "INQUIRY", pageable);
            case "content" -> boardRepository.searchByContentForUsers(text, "INQUIRY", pageable);
            default -> boardRepository.findByCategory("GENERAL", pageable);
        };
    }

    public Board save(Board board) {
        return boardRepository.save(board);
    }

    // 게시글 숨기기 처리 (삭제는 아니고, 숨김 처리만)
    public void hideBoard(Long b_idx) {
        Board board = boardRepository.findById(b_idx)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        board.setIsDeleted("Y"); // 삭제된 상태로 표시 (실제 삭제는 아님)
        boardRepository.save(board); // 상태 업데이트
    }

    // 신고 상태 해결 처리 (신고된 게시글 상태를 해결)
    public void resolveReport(Long b_idx) {
        Board board = boardRepository.findById(b_idx)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        if ("R".equals(board.getIsDeleted()) || "Y".equals(board.getIsDeleted())) {
            board.setIsDeleted("N"); // 신고 상태 해결
            board.incrementBoardReport(true);
            boardRepository.save(board); // 상태 업데이트
        } else {
            throw new IllegalArgumentException("이 게시글은 신고되지 않았습니다.");
        }
    }
}
