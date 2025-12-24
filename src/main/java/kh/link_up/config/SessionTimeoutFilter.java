package kh.link_up.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@WebFilter(value = "/*", asyncSupported = true) // 비동기 지원 활성화
@Slf4j
public class SessionTimeoutFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain filterChain) throws IOException, ServletException, ClassCastException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = httpRequest.getSession(false); // 기존 세션을 가져옴 (없으면 새로 생성 안함)
        if (session != null) {
            // 세션 만료 시간 계산
            long currentTime = System.currentTimeMillis();
            long sessionCreationTime = session.getCreationTime();
            int maxInactiveInterval = session.getMaxInactiveInterval();

            // 세션 남은 시간 계산
            long elapsedTime = currentTime - sessionCreationTime;
            long remainingTimeInMillis = maxInactiveInterval * 1000L - elapsedTime;
            if (remainingTimeInMillis < 0) {
                remainingTimeInMillis = 0;
            }

            // 밀리초를 분과 초로 변환
            long remainingTimeInMinutes = remainingTimeInMillis / 1000 / 60; // 남은 시간(분)
            long remainingTimeInSeconds = (remainingTimeInMillis / 1000) % 60; // 남은 시간(초)

            // 로그 출력
            log.info("요청 처리 중 남은 세션 시간: {}분 {}초", remainingTimeInMinutes, remainingTimeInSeconds);

            if (remainingTimeInMinutes <= 0 && remainingTimeInSeconds <= 0) {
                log.info("세션이 만료되었습니다.");
            }
        } else {
            log.debug("세션없음");
        }

        // 요청을 계속해서 처리하도록 필터 체인에 전달
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
