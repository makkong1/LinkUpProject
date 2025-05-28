package kh.link_up.service;

import kh.link_up.converter.BoardConverter;
import kh.link_up.domain.Board;
import kh.link_up.domain.SocialUser;
import kh.link_up.domain.Users;
import kh.link_up.dto.BoardDTO;
import kh.link_up.dto.BoardListDTO;
import kh.link_up.dto.BoardListDTOWrapper;
import kh.link_up.repository.BoardRepository;
import kh.link_up.repository.SocialUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardConverter boardConverter;
    private final UsersService usersService;
    private final SocialUserRepository socialUserRepository;
    private final BoardCacheService boardCacheService;

    // 전체 게시물을 페이징 처리하여 가져오는 메서드 (관리자 전용)
    public Page<Board> getAllPagesBoards(Pageable pageable) {
        return boardRepository.findAll(pageable);
    }

    // 게시글 페이징 가져오기 (유저용)
    public Page<Board> getFilteredBoardsForUser(String selectValue, String text, Pageable pageable) {
        selectValue = selectValue.trim();
        text = text.trim();

        // 검색 조건을 한 메소드에서 처리
        return boardRepository.searchByCriteria(selectValue, text, "INQUIRY", pageable);
    }

    //일반버전 조회
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
            // 'INQUIRY' 카테고리를 제외한 나머지 게시글을 페이징 처리하여 가져옵니다.
            filteredBoards = boardRepository.findByCategory("GENERAL", pageable);
        }

        // 나머지 게시글을 BoardListDTO로 변환
        List<BoardListDTO> allBoards = new ArrayList<>(noticeBoards);  // 공지사항은 이미 BoardListDTO
        allBoards.addAll(filteredBoards.getContent().stream().map(BoardListDTO::new).toList());  // 나머지 게시글도 DTO로 변환

        // 합쳐진 게시글 리스트를 다시 Page 객체로 변환
        return new PageImpl<>(allBoards, pageable, filteredBoards.getTotalElements() + noticeBoards.size());
    }

    public Optional<BoardDTO> getBoardById(Long id) {
        Optional<Board> optionalBoard = boardRepository.findById(id);

        if (optionalBoard.isPresent()) {
            Board board = optionalBoard.get();
            log.info("헤헷 filePath : {}, fileName : {}", board.getFilePath(), board.getFileName());
            board.incrementViewCount(false); // 조회수 증가
            boardRepository.save(board); // 변경된 조회수 저장
            return Optional.of(boardConverter.convertToDTO(board));
        }

        return Optional.empty(); // 조회된 게시글 반환
    }

    /**
     * 작성자를 할당하고 게시글을 저장하는 로직
     */
    @Transactional
    public void assignWriterAndSaveBoard(Board board, Principal principal, Authentication authentication) {
        boolean isSaved = false;

        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            String nickname = principal.getName();
            log.debug("nickname: {}", nickname);

            Users writer = usersService.findByNickname(nickname);
            if (writer != null) {
                board.setWriter(writer);
                boardRepository.save(board);
                isSaved = true;
                log.debug("일반 사용자 게시글 저장됨");
            } else {
                log.debug("사용자 정보를 찾을 수 없습니다.");
            }
        } else if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            OAuth2User oAuth2User = oauth2Authentication.getPrincipal();
            log.debug("OAuth2 User: {}", oAuth2User.toString());

            Map<String, Object> attributes = oAuth2User.getAttributes();
            log.debug("소셜정보 : {}", attributes);
            String oAuth2UserEmail = (String) attributes.get("email");
            log.debug("소셜 로그인 이메일: {}", oAuth2UserEmail);

            SocialUser socialUserEmail = socialUserRepository.findByEmail(oAuth2UserEmail);
            log.debug("게시글 작성 socialUserEmail: {}", socialUserEmail);

            if (socialUserEmail != null) {
                board.setSocialUser(socialUserEmail);
                boardRepository.save(board);
                isSaved = true;
                log.debug("소셜 사용자 게시글 저장됨");
            } else {
                log.debug("소셜 사용자 정보를 찾을 수 없습니다.");
            }
        } else {
            log.debug("인증 정보가 잘못되었습니다.");
        }

        // 게시글 저장 성공 시 캐시 삭제 (return 없이 무조건 실행됨)
        if (isSaved && "NOTICE".equalsIgnoreCase(board.getCategory())) {
            boardCacheService.clearNoticeBoardCache();
        }
    }

    @Transactional
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

    @Transactional
    // 좋아요 증가 로직
    public void increaseLikeCount(Long bIdx) {
        Board board = boardRepository.findById(bIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글을 찾을 수 없습니다. ID: " + bIdx));
        board.increaseLikeCount();
        boardRepository.save(board); // 변경 사항 저장
    }

    // 싫어요 증가 로직
    @Transactional
    public void increaseDislikeCount(Long bIdx) {
        Board board = boardRepository.findById(bIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글을 찾을 수 없습니다. ID: " + bIdx));
        board.decreaseDislikeCount();
        boardRepository.save(board); // 변경 사항 저장
    }


    @Transactional
    public Board save(Board board) {
        return boardRepository.save(board);
    }

    // 게시글 파일 저장
    @Transactional
    public void saveFilePath(Long bIdx, String filePath) {
        Board board = boardRepository.findById(bIdx).get();
        if (board != null) {
            board.setFilePath(filePath);
            boardRepository.save(board);
        } else {
            throw new IllegalArgumentException("게시글 파일 저장 오류발생");
        }
    }

    // 게시글 숨기기 처리 (삭제는 아니고, 숨김 처리만)
    @Transactional
    public void hideBoard(Long b_idx) {
        Board board = boardRepository.findById(b_idx)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        board.setIsDeleted("Y"); // 삭제된 상태로 표시 (실제 삭제는 아님)
        boardRepository.save(board); // 상태 업데이트
    }

    // 신고 상태 해결 처리 (신고된 게시글 상태를 해결)
    @Transactional
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
