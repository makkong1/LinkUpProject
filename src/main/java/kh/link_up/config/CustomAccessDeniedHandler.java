package kh.link_up.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
//       **AccessDeniedHandler**는 인증된 사용자가 권한이 없는 리소스에 접근할 때 동작하며,
//       권한 부족 메시지나 JSON 형식으로 메시지를 반환합니다.
//       권한 실패 처리
        log.info("CustomAccessDeniedHandler invoked");

        // 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication instanceof UsernamePasswordAuthenticationToken){
            log.debug("form Authentication: {}", authentication);
        }else if(authentication instanceof OAuth2AuthenticationToken){
            log.debug("oauth2 Authentication : {}", authentication);
        }

        // 상태 코드 기본값 403 (권한 없음)으로 설정
        int statusCode = HttpServletResponse.SC_FORBIDDEN;
        String message = "접근권한이 없습니다.";

        if (authentication != null) {
            // 사용자가 관리자나 서브 관리자인지 체크
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            boolean isSubAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUB_ADMIN"));

            if (isAdmin) {
                log.info("메인 관리자 권한 있음");
                message = "접근권한이 없습니다."; // 메인 관리자
            } else if (isSubAdmin) {
                log.info("서브 관리자 권한 있음");
                message = "접근권한이 없는 유저입니다."; // 서브 관리자
            } else {
                log.info("관리자 권한 없음");
                message = "접근권한이 없는 유저입니다."; // 일반 사용자
            }
        } else {
            log.warn("인증되지 않은 사용자 접근 시도");
            // 인증되지 않은 사용자는 로그인 필요 상태로 처리
            statusCode = HttpServletResponse.SC_UNAUTHORIZED; // 로그인 필요 상태
            message = "로그인이 필요합니다.";
        }

        // 로그인 상태에 따라 메시지를 설정하고 응답 코드 설정
        request.setAttribute("msg", message);
        response.setStatus(statusCode);

        // AJAX 요청일 경우 401이나 403을 처리할 수 있게 리디렉션이 아니라 JSON 응답을 보냄
        if (isAjaxRequest(request)) {
            log.info("이거는 json으로 응답함");
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"" + message + "\"}");
            log.info("응답 : {}", response.getWriter().toString());
        } else {
            request.getRequestDispatcher("/err/denied-page").forward(request, response); // 권한 거부 페이지로 포워드
        };
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String header = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(header);
    }
}




