package kh.link_up.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kh.link_up.domain.Board;
import kh.link_up.domain.Users;
import kh.link_up.dto.CommentDTO;
import kh.link_up.dto.SocialUserDTO;
import kh.link_up.dto.UsersDTO;
import kh.link_up.service.BoardService;
import kh.link_up.service.CommentService;
import kh.link_up.service.SocialUserService;
import kh.link_up.service.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/admin") // 어드민 기본 경로
@RequiredArgsConstructor(onConstructor_ = { @Autowired })
@Tag(name = "Admin", description = "관리자 관련 API (사용자, 게시판, 댓글 관리)")
public class AdminController {

    private final UsersService usersService;
    private final BoardService boardService;
    private final SocialUserService socialUserService;
    private final CommentService commentService;

    @PreAuthorize("hasRole('SUB_ADMIN') or hasRole('ADMIN')")
    @GetMapping("/listForAdminP")
    @Operation(summary = "관리자 게시판 및 댓글 리스트 페이지", description = "게시글 및 신고 댓글 리스트를 페이징, 필터링해서 조회")
    public String boardListPFromAdmin(Model model,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "select_value", defaultValue = "all") String selectValue,
            @RequestParam(value = "text", defaultValue = "") String text,
            @RequestParam(value = "select_comment_value", defaultValue = "all_comment") String selectComment,
            @RequestParam(value = "text_comment", defaultValue = "") String textComment) {

        Pageable boardPageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("uploadTime")));
        Pageable commentPageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("cReport")));

        // 게시글 검색 기능
        Page<Board> boardList;
        if ("all".equals(selectValue) || text.trim().isEmpty()) {
            boardList = boardService.getAllPagesBoards(boardPageable);
            model.addAttribute("boardList", boardList);
            log.info("admin Page board : {}", boardPageable.getPageNumber());
        } else {
            boardList = boardService.getFilteredBoards(selectValue, text, boardPageable);
            model.addAttribute("boardList", boardList);
        }

        // 댓글 검색 기능
        Page<CommentDTO> commentList;
        if ("all_comment".equals(selectComment) || textComment.trim().isEmpty()) {
            // 신고당한 댓글만 가져옴
            commentList = commentService.getReportComment(commentPageable);
            model.addAttribute("commentList", commentList);
        } else {
            model.addAttribute("commentList",
                    commentService.getFilteredComments(selectComment, textComment, commentPageable));
        }

        return "admin/listForAdmin";
    };

    // 관리자 유저페이지로 이동
    @Operation(summary = "관리자 대시보드", description = "관리자용 사용자 리스트 및 소셜 사용자 리스트 조회")
    @PreAuthorize("hasRole('SUB_ADMIN') or hasRole('ADMIN')")
    @GetMapping
    public String adminDashboardFromAdmin(Model model) {
        log.debug("dkdkd : {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        // 모든 사용자 리스트 조회
        List<UsersDTO> users = usersService.getAllUsersFromAdmin();
        List<SocialUserDTO> social_users = socialUserService.getAllSocialUsersFromAdmin();
        model.addAttribute("users", users);
        model.addAttribute("social_users", social_users);
        return "admin/a_main"; // 관리자 대시보드 페이지 반환
    };

    // 사용자 블랙
    @Operation(summary = "사용자 차단", description = "사용자를 블랙리스트에 등록하여 차단")
    @PreAuthorize("hasRole('SUB_ADMIN') or hasRole('ADMIN')")
    @PostMapping("/users/block/{id}")
    public @ResponseBody String blockUserFromAdmin(@PathVariable("id") String id,
            @RequestParam("blockReason") String blockReason) {
        Optional<Users> user = usersService.findByUId(id);

        if (user.isPresent()) {
            Users foundUser = user.get();
            foundUser.setUBlockReason(blockReason);
            usersService.blockUser(foundUser);
            return "success";
        }
        return "error";
    };

    // 블랙취소
    @Operation(summary = "사용자 차단 해제", description = "블랙리스트에서 사용자 차단 해제")
    @PreAuthorize("hasRole('SUB_ADMIN') or hasRole('ADMIN')")
    @PostMapping("/users/unblock/{id}")
    public @ResponseBody String unblockUserFromAdmin(@PathVariable("id") String id) {
        Optional<Users> user = usersService.findByUId(id);

        if (user.isPresent()) {
            Users foundUser = user.get();
            foundUser.setUBlockReason(null);
            usersService.blockUser(foundUser);
            return "success";
        }
        return "error";
    };

    // 잠금 해제 처리
    @Operation(summary = "사용자 잠금 해제", description = "잠긴 사용자의 계정 잠금 해제")
    @PreAuthorize("hasRole('SUB_ADMIN') or hasRole('ADMIN')")
    @PostMapping("/users/unlock/{userId}")
    @ResponseBody
    public String unlockUserFromAdmin(@PathVariable("userId") String userId) {
        try {
            usersService.unlockUser(userId);
            return "success"; // 상태 변경 성공
        } catch (Exception e) {
            return "error"; // 상태 변경 실패
        }
    };

    // 관리자 전환
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{id}/role")
    @Operation(summary = "관리자 권한 부여/해제", description = "사용자 권한을 ADMIN 혹은 SUB_ADMIN으로 승격/강등")
    public @ResponseBody String changeUserRoleFromAdmin(@PathVariable("id") String id,
            @RequestParam("action") String action) {
        if ("promote".equals(action)) {
            usersService.promoteToAdmin(id);
        } else if ("demote".equals(action)) {
            usersService.demoteFromAdmin(id);
        }
        return "success";
    };

    @Operation(summary = "게시글 숨김 처리", description = "게시글을 실제 삭제하지 않고 숨김 처리")
    @PreAuthorize("hasRole('SUB_ADMIN') or hasRole('ADMIN')")
    @PostMapping("/board/{b_idx}/delete")
    public ResponseEntity<String> deleteBoardFromAdmin(@PathVariable("b_idx") Long b_idx) {
        try {
            // 게시글 숨기기 처리 (실제 삭제는 아니고, 숨김 처리만)
            boardService.hideBoard(b_idx);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("처리 실패");
        }
    };

    @Operation(summary = "게시글 신고 상태 해결", description = "신고된 게시글의 신고 상태를 해제")
    @PreAuthorize("hasRole('SUB_ADMIN') or hasRole('ADMIN')")
    @PostMapping("/board/{b_idx}/resolve")
    public ResponseEntity<String> resolveReportFromAdmin(@PathVariable("b_idx") Long b_idx) {
        try {
            // 신고 상태 해결 처리
            boardService.resolveReport(b_idx);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("신고 해결 실패");
        }
    };

    @Operation(summary = "댓글 삭제", description = "관리자가 댓글을 삭제 처리")
    @PreAuthorize("hasRole('SUB_ADMIN') or hasRole('ADMIN')")
    @PostMapping("/comment/{cIdx}/delete")
    public ResponseEntity<String> deleteCommentFromAdmin(@PathVariable Long cIdx) {
        log.info("deleteComment c-Idx: {}", cIdx);
        boolean reportComment = commentService.deleteCommentForAdmin(cIdx);
        if (reportComment) {
            return ResponseEntity.ok("success");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    };

    @Operation(summary = "댓글 신고 상태 해결", description = "관리자가 신고된 댓글의 상태를 해결")
    @PreAuthorize("hasRole('SUB_ADMIN') or hasRole('ADMIN')")
    @PostMapping("/comment/{cIdx}/resolve")
    public ResponseEntity<String> resolveCommentFromAdmin(@PathVariable Long cIdx) {
        log.info("resolveComment c-Idx: {}", cIdx);
        boolean resolveComment = commentService.resolveCommentForAdmin(cIdx);
        if (resolveComment) {
            return ResponseEntity.ok("success");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    };
}