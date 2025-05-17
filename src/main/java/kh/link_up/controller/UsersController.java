package kh.link_up.controller;

import jakarta.validation.Valid;
import kh.link_up.dto.*;
import kh.link_up.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Controller
@RequestMapping("/users")
// RequiredArgsConstructor가 생성자주입을 대신해줌 (lombok 어노테이션임)
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class UsersController {

    private final UsersService usersService;
    private final NotionService notionService;
    private final SocialUserService socialUserService;

    @GetMapping("/loginP")
    public String loginPage() {
        return "users/login";
    }

    @GetMapping("/newP")
    public String registerPage() {
        return "users/user_form";
    }

    @RequestMapping("/findPwdP")
    public String findPwdP() {
        return "users/findPassword";
    }

    @PostMapping("/new")
    public String create(
            @Valid @ModelAttribute("user") UsersDTO usersDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            log.error("회원가입 유효성 검사 오류: {}", bindingResult.getAllErrors());
            return "users/user_form";
        }

        usersService.createUser(usersDTO);
        log.info("회원가입 성공: {}", usersDTO);
        return "redirect:/users";
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{uIdx}/notion")
    public String notionP(@PathVariable("uIdx") Long uIdx, Model model) {
        List<NotionDTO> notionDTOList = notionService.getAllNotion(uIdx);
        model.addAttribute("notions", notionDTOList);
        return "notion/Notion_main";
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{uIdx}/editP")
    public String userEdit(@PathVariable("uIdx") Long uIdx, Model model) {
        Optional<?> userDTO = findUserByAuthentication(uIdx);

        if (userDTO.isPresent()) {
            if (userDTO.get() instanceof SocialUserDTO socialUser) {
                model.addAttribute("socialUser", socialUser);
                model.addAttribute("isSocialLogin", true);
            } else if (userDTO.get() instanceof UsersDTO user) {
                model.addAttribute("users", user);
                model.addAttribute("isSocialLogin", false);
            }
            return "users/user_edit";
        }
        return "redirect:/users/list";
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{uIdx}/edit")
    public String userUpdate(
            @PathVariable("uIdx") Long uIdx,
            @Valid @ModelAttribute("user") UsersDTO usersDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            log.error("수정 처리 유효성 검사 오류: {}", bindingResult.getAllErrors());
            return "users/edit";
        }
        usersService.updateUser(uIdx, usersDTO);
        return "redirect:/users/list";
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/delete/{uIdx}")
    public String userDelete(@PathVariable("uIdx") Long uIdx) {
        usersService.deleteUser(uIdx);
        return "redirect:/users/list";
    }

    @PostMapping("/new/checkNickname")
    @ResponseBody
    public ResponseEntity<DuplicateCheckResponse> checkNickname(@RequestParam("nickname") String nickname) {
        boolean isDuplicate = usersService.isNicknameDuplicate(nickname);
        return ResponseEntity.ok(new DuplicateCheckResponse(isDuplicate, isDuplicate ? "이미 사용 중인 닉네임입니다." : "사용 가능한 닉네임입니다."));
    }

    @PostMapping("/new/checkId")
    @ResponseBody
    public ResponseEntity<DuplicateCheckResponse> checkId(@RequestParam("id") String id) {
        boolean isDuplicate = usersService.isUserIdDuplicate(id);
        return ResponseEntity.ok(new DuplicateCheckResponse(isDuplicate, isDuplicate ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다."));
    }

    @PostMapping("/findPassword")
    public String findPassword(@RequestParam("id") String id, Model model) {
        Optional<String> email = usersService.findEmailById(id);
        if (email.isPresent()) {
            // 비동기 작업의 결과를 CompletableFuture로 받음
            CompletableFuture<String> futureAuthCode = usersService.sendAuthCodeToEmail(id, email.get());

            // 비동기 작업이 완료될 때까지 대기하고 결과 받기
            String authCode = futureAuthCode.join();  // 결과 대기

            model.addAttribute("id", id);
            model.addAttribute("authCode", authCode);
            return "users/verifyAuthCode";
        } else {
            model.addAttribute("error", "아이디에 해당하는 이메일을 찾을 수 없습니다.");
            return "users/findPassword";
        }
    }

    @PostMapping("/verifyAuthCode")
    public String verifyAuthCode(@RequestParam("id") String id, @RequestParam("authCode") String authCode, Model model) {
        boolean isValidCode = usersService.verifyAuthCode(id, authCode);
        if (isValidCode) {
            model.addAttribute("id", id);
            return "users/changePassword";
        } else {
            model.addAttribute("error", "인증번호가 일치하지 않습니다.");
            return "users/verifyAuthCode";
        }
    }

    @PostMapping("/changePassword")
    public String changePassword(@RequestParam("id") String id, @RequestParam("password") String password, Model model) {
        boolean isUpdated = usersService.changePassword(id, password);
        if (isUpdated) {
            return "users/login";
        } else {
            model.addAttribute("error", "비밀번호 변경에 실패했습니다.");
            return "users/changePassword";
        }
    }
    
    //활동보기
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{idx}")
    public String userContent(@PathVariable("idx") Long idx,Model model){
        log.debug("userId : {}", idx);
        UsersDTO userContents = usersService.getUserContents(idx);
        model.addAttribute("userContents", userContents);
        return "users/user_contents";
    }

    // 중복 로직 제거를 위한 메소드
    private Optional<?> findUserByAuthentication(Long uIdx) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof OAuth2AuthenticationToken) {
            return socialUserService.getUserById(uIdx);
        } else if (authentication instanceof UsernamePasswordAuthenticationToken) {
            return usersService.getUserById(uIdx);
        }
        return Optional.empty();
    }

//    중복 체크 응답 DTO record클래스로 처리 자바14부터 나온기능
//    자동 생성자 및 메서드: record를 사용하면 생성자, getter 메서드,
//    equals(), hashCode(), toString() 메서드가 자동으로 생성됩니다.
//    예를 들어, 위 코드에서는 isDuplicate()와 getMessage() 메서드가 자동으로 만들어짐
    public record DuplicateCheckResponse(boolean isDuplicate, String message) {}
}
