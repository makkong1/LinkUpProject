package kh.link_up.controller;

import kh.link_up.domain.Users;
import kh.link_up.dto.CustomUserDetails;
import kh.link_up.dto.CustomOauth2User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@Slf4j
public class GlobalModelAdvice {

    // 모든 모델에 공통으로 'user' 추가 , 추가로 폼로그인객체랑 소셜로그인객체를 나눠서 넣어줌
    @ModelAttribute
    public void addCommonModelData(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            log.debug("principal {}", principal);

            // 폼 로그인 사용자 처리
            if (principal instanceof CustomUserDetails userDetails) {
                Users user = userDetails.user();
                log.debug("FormLogin user {}", user);

                // 계정 잠금 상태 확인
                if (user.isAccountLocked()) {
                    log.debug("Account is locked for user: {}", user.getUUsername());
                    return; // 계정이 잠금 상태이면 모델에 user 정보를 추가하지 않음
                }

                model.addAttribute("user", user);
                // 폼 로그인일 경우 false로 설정
                model.addAttribute("isSocial", false);
            }

            // 소셜 로그인 사용자 처리
            else if (principal instanceof CustomOauth2User customOauth2User) {
                log.debug("Oauth2Login user {}", customOauth2User.getUEmail());
                model.addAttribute("user", customOauth2User);
                // 소셜 로그인일 경우 true로 설정
                model.addAttribute("isSocial", true);
            }
        }
    }
}
