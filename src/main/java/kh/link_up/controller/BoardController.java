package kh.link_up.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kh.link_up.domain.Board;
import kh.link_up.domain.TargetType;
import kh.link_up.dto.BoardDTO;
import kh.link_up.dto.BoardListDTO;
import kh.link_up.dto.CommentDTO;
import kh.link_up.service.BoardFileService;
import kh.link_up.service.BoardService;
import kh.link_up.service.CommentService;
import kh.link_up.util.LikeDislikeUtil;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequiredArgsConstructor(onConstructor_ = { @Autowired })
@Slf4j
@PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUB_ADMIN')")
@Tag(name = "Board", description = "게시판 관련 API")
public class BoardController {

    private final BoardService boardService;
    private final CommentService commentService;
    private final BoardFileService boardFileService;
    private final UserUtil userUtil;
    private final LikeDislikeUtil likeDislikeUtil;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "게시글 목록 조회", description = "검색 조건과 페이징 정보를 받아 게시글 목록을 반환합니다.")
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
        log.debug("boardPage : {}", boardPage.getTotalPages());
        return "board/list";
    }

    // 새로운 게시글 작성
    @GetMapping("/new")
    @Operation(summary = "게시글 작성 폼 조회", description = "새 게시글 작성 폼을 반환합니다.")
    public String createForm(Model model) {
        model.addAttribute("board", new Board());
        return "board/form"; // 게시글 작성 폼
    }

    @PostMapping("/save")
    @Operation(summary = "게시글 저장", description = "새 게시글과 첨부파일을 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "게시글 저장 성공 시 게시글 목록으로 리다이렉트"),
            @ApiResponse(responseCode = "500", description = "파일 저장 실패 시 게시글 목록으로 리다이렉트", content = @Content)
    })
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

        return "redirect:/board"; // 성공 시 목록으로 가가
    }

    @GetMapping("/{bIdx}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "게시글 상세 조회", description = "게시글 ID로 게시글과 댓글 목록을 조회합니다.")
    public String view(@PathVariable("bIdx") Long id, Model model, Pageable pageable) {
        // 기본적으로 pageable이 전달되지만, 한 페이지에 댓글을 10개씩 보여주도록 설정
        pageable = PageRequest.of(pageable.getPageNumber(), 10, Sort.by(Sort.Order.desc("cUpload"))); // cUpLoad 기준 내림차순
                                                                                                      // 정렬

        // 게시글 ID로 게시글 조회
        BoardDTO board = boardService.getBoardById(id).orElse(null);

        // 게시글에 대한 페이징된 댓글 조회
        Page<CommentDTO> commentsByBoard = commentService.getCommentsAll(id, pageable);

        // 모델에 데이터 담기
        model.addAttribute("comments", commentsByBoard);
        model.addAttribute("board", board);

        boardService.increaseViewCount(id);

        return "board/view"; // 게시글 상세 보기
    }

    // 게시글 삭제하기
    @Operation(summary = "게시글 삭제", description = "게시글 ID로 게시글을 삭제합니다.")
    @DeleteMapping("/{bIdx}")
    public String delete(@PathVariable("bIdx") Long id) {
        boardService.deleteBoard(id);
        return "redirect:/board"; // 게시글 목록으로 리다이렉트
    }

    @PostMapping("/report/{bIdx}")
    @ResponseBody
    @Operation(summary = "게시글 신고", description = "게시글을 신고합니다.")
    public String boardReport(@PathVariable("bIdx") Long id) {
        log.debug("boardReport id : {}", id);

        boolean isReported = boardService.reportBoard(id);

        if (isReported) {
            return "success"; // 성공 시 응답
        }

        return "failure"; // 실패 시 응답
    }

    // 좋아요 증가
    @Operation(summary = "좋아요 증가", description = "게시글 좋아요 수를 1 증가시킵니다.")
    // @PostMapping("/{bIdx}/like")
    // public ResponseEntity<Map<String, Long>> increaseLikeCount(@PathVariable Long
    // bIdx) {
    // if (bIdx == null || bIdx <= 0) {
    // throw new IllegalArgumentException("유효하지 않은 게시글 번호입니다.");
    // }
    //
    // try {
    // // Redis에 좋아요 수 1 증가
    // likeDislikeCacheService.increaseLikeCount(bIdx);
    //
    // // DB에서 현재 좋아요 수 조회 (필요 최소한)
    // long dbLike = boardRepository.findById(bIdx)
    // .map(board -> (long) board.getLikeCount())
    // .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
    //
    // // Redis에서 증가된 좋아요 수 가져오기
    // long redisLike = likeDislikeCacheService.getLikeCount(bIdx);
    //
    // Map<String, Long> result = Map.of("likeCount", dbLike + redisLike);
    // log.debug("LIKE 응답 : {}", result);
    // return ResponseEntity.ok(result);
    //
    // } catch (IllegalArgumentException e) {
    // throw e; // GlobalExceptionHandler가 처리
    // } catch (Exception e) {
    // log.error("좋아요 증가 중 오류 발생", e);
    // throw new RuntimeException("좋아요 처리 중 오류가 발생했습니다.");
    // }
    // }
    @PostMapping("/{bIdx}/like")
    public ResponseEntity<Map<String, Long>> increaseBoardLike(@PathVariable Long bIdx) {
        return likeDislikeUtil.likeDislikeprocess(TargetType.BOARD, bIdx, true);
    }

    // 싫어요 증가
    @Operation(summary = "싫어요 증가", description = "게시글 싫어요 수를 1 증가시킵니다.")
    // @PostMapping("/{bIdx}/dislike")
    // public ResponseEntity<Map<String, Long>> increaseDislikeCount(@PathVariable
    // Long bIdx) {
    // if (bIdx == null || bIdx <= 0) {
    // throw new IllegalArgumentException("유효하지 않은 게시글 번호입니다.");
    // }
    //
    // try {
    // likeDislikeCacheService.increaseDislikeCount(bIdx);
    //
    // long dbDislike = boardRepository.findById(bIdx)
    // .map(board -> (long) board.getDislikeCount())
    // .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
    //
    // long redisDislike = likeDislikeCacheService.getDislikeCount(bIdx);
    //
    // Map<String, Long> result = Map.of("dislikeCount", dbDislike + redisDislike);
    // log.info("DISLIKE 응답: {}", result);
    // return ResponseEntity.ok(result);
    //
    // } catch (IllegalArgumentException e) {
    // throw e;
    // } catch (Exception e) {
    // log.error("싫어요 증가 중 오류 발생", e);
    // throw new RuntimeException("싫어요 처리 중 오류가 발생했습니다.");
    // }
    // }
    @PostMapping("/{bIdx}/dislike")
    public ResponseEntity<Map<String, Long>> increaseBoardDislike(@PathVariable Long bIdx) {
        return likeDislikeUtil.likeDislikeprocess(TargetType.BOARD, bIdx, false);
    }
}
