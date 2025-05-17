package kh.link_up.controller;

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
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class AdminController {

    private final UsersService usersService;
    private final BoardService boardService;
    private final SocialUserService socialUserService;
    private final CommentService commentService;

    @GetMapping("/listForAdminP")
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
        if ("all".equals(selectValue) || text.isEmpty()) {
            boardList = boardService.getAllPagesBoards(boardPageable);
            model.addAttribute("boardList", boardList);
            log.info("admin Page board : {}", boardPageable.getPageNumber());
        } else {
            boardList = boardService.getFilteredBoards(selectValue, text, boardPageable);
            model.addAttribute("boardList", boardList);
        }

        // 댓글 검색 기능
        Page<CommentDTO> commentList;
        if ("all_comment".equals(selectComment) || textComment.isEmpty()) {
            // 신고당한 댓글만 가져옴
            commentList = commentService.getReportComment(commentPageable);
            model.addAttribute("commentList", commentList);
        } else {
            model.addAttribute("commentList", commentService.getFilteredComments(selectComment, textComment, commentPageable));
        }

        return "admin/listForAdmin";
    };

    // 관리자 유저페이지로 이동
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

    //사용자 블랙
    @PreAuthorize("hasRole('SUB_ADMIN') or hasRole('ADMIN')")
    @PostMapping("/users/block/{id}")
    public @ResponseBody String blockUserFromAdmin(@PathVariable("id") String id, @RequestParam("blockReason") String blockReason) {
        Optional<Users> user = usersService.findByUId(id);

        if (user.isPresent()) { 
            Users foundUser = user.get();
            foundUser.setUBlockReason(blockReason);
            usersService.blockUser(foundUser);
            return "success";
        }
        return "error";
    };
    
    //블랙취소
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

    @PreAuthorize("hasRole('SUB_ADMIN') or hasRole('ADMIN')")
    // 잠금 해제 처리
    @PostMapping("/users/unlock/{userId}")
    @ResponseBody
    public String unlockUserFromAdmin(@PathVariable("userId") String userId) {
        try {
            usersService.unlockUser(userId);
            return "success";  // 상태 변경 성공
        } catch (Exception e) {
            return "error";  // 상태 변경 실패
        }
    };

    //관리자 전환
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{id}/role")
    public @ResponseBody String changeUserRoleFromAdmin(@PathVariable("id") String id, @RequestParam("action") String action) {
        if ("promote".equals(action)) {
            usersService.promoteToAdmin(id);
        } else if ("demote".equals(action)) {
            usersService.demoteFromAdmin(id);
        }
        return "success";
    };
    
    // 게시글 삭제처리
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

    // 게시글 복구 
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

    //관리자 댓글 삭제
    @PreAuthorize("hasRole('SUB_ADMIN') or hasRole('ADMIN')")
    @PostMapping("/comment/{cIdx}/delete")
    public ResponseEntity<String> deleteCommentFromAdmin(@PathVariable Long cIdx){
        log.info("deleteComment c-Idx: {}", cIdx);
        boolean reportComment = commentService.deleteCommentForAdmin(cIdx);
        if(reportComment){
            return ResponseEntity.ok("success");
        }else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    };

    //관리자 댓글 복원
    @PreAuthorize("hasRole('SUB_ADMIN') or hasRole('ADMIN')")
    @PostMapping("/comment/{cIdx}/resolve")
    public ResponseEntity<String> resolveCommentFromAdmin(@PathVariable Long cIdx){
        log.info("resolveComment c-Idx: {}", cIdx);
        boolean resolveComment = commentService.resolveCommentForAdmin(cIdx);
        if(resolveComment){
            return ResponseEntity.ok("success");
        }else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    };
}