package kh.link_up.config;

import jakarta.servlet.ServletException;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;

import java.io.IOException;

@Configuration
public class CustomSessionExpiredStrategy implements SessionInformationExpiredStrategy {

    @Override
    public void onExpiredSessionDetected(SessionInformationExpiredEvent event) throws IOException, ServletException {
        // 세션에 세션 만료 메시지를 저장
        event.getRequest().getSession().setAttribute("sessionExpiredMessage", "세션이 만료되었습니다. 다시 로그인해주세요.");

        // 로그인 페이지로 리다이렉트
        event.getResponse().sendRedirect("/users/loginP");
    }
}
