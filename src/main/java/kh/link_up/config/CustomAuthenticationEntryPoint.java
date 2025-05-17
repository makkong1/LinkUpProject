package kh.link_up.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint{
//    **AuthenticationEntryPoint**는 인증되지 않은 사용자가 접근할 때 동작하며 로그인 페이지로 리디렉션하거나 JSON 형식으로 로그인 필요 메시지를 반환합니다.
    // like 인증인듯?
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.info("AuthenticationEntryPoint invoked");

        // 인증되지 않은 사용자가 요청했을 때
        if (isAjaxRequest(request)) {
            // AJAX 요청일 경우 401 응답을 JSON 형식으로 반환
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"로그인이 필요합니다.\"}");
        };
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String header = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(header);
    };
}
