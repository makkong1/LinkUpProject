package kh.link_up.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    public CustomLogoutSuccessHandler() {
        log.debug("CustomLogoutSuccessHandler 생성됨");
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // 인증 정보 삭제
        SecurityContextHolder.clearContext();
        log.debug("SecurityContext 인증 정보 삭제됨");

        // 세션에서 사용자 정보 삭제
        HttpSession session = request.getSession(false); // 세션이 존재하지 않으면 null 반환
        if (session != null) {
            session.invalidate(); // 세션 무효화
            log.debug("세션 정보 삭제됨");
        } else {
            log.warn("세션이 존재하지 않습니다.");
        }

        // 로그아웃 후 리다이렉트
        try {
            log.debug("로그아웃 성공 후 리다이렉트 중...");
            response.sendRedirect("/users/loginP"); // 로그아웃 후 로그인 페이지로 리다이렉트
        } catch (IOException e) {
            log.error("리다이렉트 중 IOException 발생: {}", e.getMessage());
        }
    }

}

