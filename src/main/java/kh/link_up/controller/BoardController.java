package kh.link_up.controller;

import kh.link_up.domain.Board;
import kh.link_up.dto.BoardDTO;
import kh.link_up.dto.BoardListDTO;
import kh.link_up.dto.CommentDTO;
import kh.link_up.repository.BoardRepository;
import kh.link_up.service.BoardFileService;
import kh.link_up.service.BoardService;
import kh.link_up.service.CommentService;
import kh.link_up.service.LikeDislikeCacheService;
import kh.link_up.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = { "/board" })
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
public class BoardController {

    private final BoardService boardService;
    private final CommentService commentService;
    private final LikeDislikeCacheService likeDislikeCacheService;
    private final BoardRepository boardRepository;
    private final BoardFileService boardFileService;
    private final UserUtil userUtil;

    @GetMapping
    public String list(
            @RequestParam(value = "select_value", required = false, defaultValue = "all") String selectValue,
            @RequestParam(value = "text", required = false, defaultValue = "") String text,
            @PageableDefault(size = 10, sort = "uploadTime", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {

        // 페이징된 게시글을 담을 변수
        Page<BoardListDTO> boardPage = Page.empty(); // boardPage를 미리 초기화

        // 페이지 번호와 크기 로그 찍기
        log.debug("Page number: {}", pageable.getPageNumber());
        log.debug("Page size: {}", pageable.getPageSize());

        // 검색 조건이 있을 경우, 필터링된 페이징된 게시글을 가져오기
        if (!text.isEmpty() && !selectValue.equals("all")) {
            boardPage = boardService.getAllPagesBoardsForUsers(selectValue, text, pageable); // BoardListDTO를 반환
        } else {
            // 조건이 맞지 않으면 기본 데이터를 설정할 수 있음 (필요에 따라)
            boardPage = boardService.getAllPagesBoardsForUsers("all", "", pageable); // BoardListDTO를 반환
        }
        // 뷰에 페이징된 게시글 전달
        model.addAttribute("boardPage", boardPage); // BoardListDTO 타입으로 전달
        return "board/list"; // Thymeleaf 템플릿 파일 경로
    }

    // 새로운 게시글 작성
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("board", new Board());
        return "board/form"; // 게시글 작성 폼
    }

    @PostMapping("/save")
    public String create(@ModelAttribute Board board,
                         @RequestParam("files") List<MultipartFile> files,
                         Principal principal,
                         Authentication authentication) {
        try {
            // 1. 작성자 할당 및 게시글 저장
            boardFileService.assignWriterAndSaveBoard(board, principal, authentication);

            // 2. 파일이 존재하는 경우만 처리
            if (files != null && !files.isEmpty()) {
                String userNickname = userUtil.getUserNickname(principal);
                String boardTitle = board.getTitle();
                log.debug("board create userNickname: {}", userNickname);

                // 3. 파일 저장 위임
                boardFileService.saveFiles(board.getBIdx(), userNickname, boardTitle, files);
            }

        } catch (IOException e) {
            log.error("파일 저장 중 오류 발생: {}", e.getMessage(), e);
            return "redirect:/board"; // 파일 저장 오류 시 목록으로
        } catch (Exception e) {
            log.debug("작성자 정보가 없어 게시글 저장 실패 :{}", e.getMessage());
            return "redirect:/board"; // 작성자 정보 없음 시 목록으로
        }

        return "redirect:/board"; // 성공 시 목록으로
    }

    @GetMapping("/{bIdx}")
    public String view(@PathVariable("bIdx") Long id, Model model, Pageable pageable) {
        // 기본적으로 pageable이 전달되지만, 한 페이지에 댓글을 10개씩 보여주도록 설정
        pageable = PageRequest.of(pageable.getPageNumber(), 10, Sort.by(Sort.Order.desc("cUpload"))); // cUpLoad 기준 내림차순 정렬

        // 게시글 ID로 게시글 조회
        BoardDTO board = boardService.getBoardById(id).orElse(null);

        // 게시글에 대한 페이징된 댓글 조회
        Page<CommentDTO> commentsByBoard = commentService.getCommentsAll(id, pageable);

        // 모델에 데이터 담기
        model.addAttribute("comments", commentsByBoard);
        model.addAttribute("board", board);

        return "board/view"; // 게시글 상세 보기
    }

    // 게시글 삭제하기
    @DeleteMapping("/{bIdx}")
    public String delete(@PathVariable("bIdx") Long id) {
        boardService.deleteBoard(id);
        return "redirect:/board"; // 게시글 목록으로 리다이렉트
    }

    @PostMapping("/report/{bIdx}")
    @ResponseBody
    public String boardReport(@PathVariable("bIdx") Long id) {
        log.debug("boardReport id : {}", id);

        boolean isReported = boardService.reportBoard(id);

        if (isReported) {
            return "success"; // 성공 시 응답
        }

        return "failure"; // 실패 시 응답
    }

    // 좋아요 증가
    @PostMapping("/{bIdx}/like")
    public ResponseEntity<Map<String, Long>> increaseLikeCount(@PathVariable Long bIdx) {
        // 캐시에 1 증가
        likeDislikeCacheService.increaseLikeCount(bIdx);

        // DB에서 기존 값 가져오기 (없으면 0)
        long dbLike = boardRepository.findById(bIdx)
                .map(board -> (long) board.getLikeCount())
                .orElse(0L);

        // Redis 값 가져오기
        long redisLike = likeDislikeCacheService.getLikeCount(bIdx);

        // 총합 전달
        Map<String, Long> result = Map.of("likeCount", redisLike + dbLike);
        log.debug("LIKE 응답 : {}", result);
        return ResponseEntity.ok(result);
    }

    // 싫어요 증가
    @PostMapping("/{bIdx}/dislike")
    public ResponseEntity<Map<String, Long>> increaseDislikeCount(@PathVariable Long bIdx) {
        likeDislikeCacheService.increaseDislikeCount(bIdx);

        long dbDislike = boardRepository.findById(bIdx)
                .map(board -> (long) board.getDislikeCount())
                .orElse(0L);

        long redisDislike = likeDislikeCacheService.getDislikeCount(bIdx);

        Map<String, Long> result = Map.of("dislikeCount", dbDislike + redisDislike);
        log.info("DISLIKE 응답: {}", result);
        return ResponseEntity.ok(result);
    }

}
