package kh.link_up.service;

import jakarta.transaction.Transactional;
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

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
@Transactional
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
    public void assignWriterAndSaveBoard(Board board, Principal principal, Authentication authentication) {
        boolean isSaved = false;
        // 일반 로그인 사용자의 경우
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            String nickname = principal.getName(); // 로그인된 사용자 정보
            log.debug("nickname: {}", nickname);
            Users writer = usersService.findByNickname(nickname);

            if (writer != null) {
                board.setWriter(writer); // 일반 사용자는 writer 필드에 저장
                boardRepository.save(board); // 게시글 저장
                isSaved = true;
                log.debug("일반 사용자 게시글 저장됨");
            } else {
                log.debug("사용자 정보를 찾을 수 없습니다.");
            }
            return;
        }

        // 소셜 로그인 사용자의 경우
        else if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            OAuth2User oAuth2User = oauth2Authentication.getPrincipal();
            log.debug("OAuth2 User: {}", oAuth2User.toString());

//            소셜로그인으로 게시글을 저장하면 오류가 발생하는데
//            지금 오류가 나는게 소셜유저에 같은 이름이 2개가 있어서 그런듯한데 그럼 이름말고 다른걸로 고려해야된다
//            작성자 정보가 없어 게시글 저장 실패 :Query did not return a unique result: 2 results were returned

//            OAuth2User에서 속성 정보 가져오기
            Map<String, Object> attributes = oAuth2User.getAttributes();
            String oAuth2UserEmail = (String) attributes.get("email"); // 이메일 가져오기
            log.debug("소셜 로그인 이메일: {}", oAuth2UserEmail);
//
//            이메일로 소셜 사용자 조회
            SocialUser socialUserEmail = socialUserRepository.findByEmail(oAuth2UserEmail);
            log.debug("게시글 작성 socialUserEmail: {}", socialUserEmail);

            // 소셜 로그인 사용자 정보를 처리하는 방식
//            String socialId = oAuth2User.getName(); // 예: 소셜 로그인에서 받은 고유 ID
//            SocialUser socialUser = socialUserRepository.findByName(socialId); // DB에서 해당 소셜 사용자 찾기
//            log.debug("게시글 작성 socialUser: {}", socialUser);

            if (socialUserEmail != null) {
                board.setSocialUser(socialUserEmail); // 소셜 로그인 사용자는 socialUser 필드에 저장
                boardRepository.save(board); // 게시글 저장
                isSaved = true;
                log.debug("소셜 사용자 게시글 저장됨");
            } else {
                log.debug("소셜 사용자 정보를 찾을 수 없습니다.");
            }
            return;
        }

        // 캐시 삭제
        if (isSaved && "NOTICE".equalsIgnoreCase(board.getCategory())) {
            boardCacheService.clearNoticeBoardCache(); // ← 여기서 캐시 무효화
        }
        // 로그인 정보가 없거나, 인증 정보가 잘못된 경우 처리
        log.debug("인증 정보가 잘못되었습니다.");
    }

    public void deleteBoard(Long id) {
        boardRepository.deleteById(id);
    }

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

    // 좋아요 증가 로직
    public void increaseLikeCount(Long bIdx) {
        Board board = boardRepository.findById(bIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글을 찾을 수 없습니다. ID: " + bIdx));
        board.increaseLikeCount();
        boardRepository.save(board); // 변경 사항 저장
    }

    // 싫어요 증가 로직
    public void increaseDislikeCount(Long bIdx) {
        Board board = boardRepository.findById(bIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글을 찾을 수 없습니다. ID: " + bIdx));
        board.decreaseDislikeCount();
        boardRepository.save(board); // 변경 사항 저장
    }

    // 게시글 페이징 가져오기 (유저용)
    public Page<Board> getFilteredBoardsForUser(String selectValue, String text, Pageable pageable) {
        selectValue = selectValue.trim();
        text = text.trim();

        // 검색 조건을 한 메소드에서 처리
        return boardRepository.searchByCriteria(selectValue, text, "INQUIRY", pageable);
    }

    public Board save(Board board) {
        return boardRepository.save(board);
    }

    // 게시글 파일 저장
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
