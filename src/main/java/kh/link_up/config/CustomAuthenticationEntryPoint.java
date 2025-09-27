package kh.link_up.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint{
//    **AuthenticationEntryPoint**는 인증되지 않은 사용자가 접근할 때 동작하며
//    로그인 페이지로 리디렉션하거나 JSON 형식으로 로그인 필요 메시지를 반환합니다.
//    인증 실패 처리
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.info("EntryPoint invoked for URI: {}", request.getRequestURI());

        if (isAjaxRequest(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"로그인이 필요합니다.\"}");
        } else {
            // 일반 요청일 경우 세션에 메시지 저장 후 리다이렉트
            request.getSession().setAttribute("msg", "로그인이 필요한 페이지입니다.");
            request.getSession().setAttribute("action", "login");
            response.sendRedirect("/err/denied-page"); // 또는 /login 으로 설정 가능
            log.debug("메세지 : {}", request.getSession().getAttribute("action"));
        }
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String header = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(header);
    };
}
