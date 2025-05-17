package kh.link_up.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kh.link_up.domain.Users;
import kh.link_up.repository.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final UsersRepository usersRepository;

    private static final String INVALID_CREDENTIALS_MESSAGE = "아이디나 비밀번호가 틀립니다.";
    private static final String ACCOUNT_LOCKED_MESSAGE = "계정이 잠겼습니다. 관리자에게 문의하거나 비밀번로를 변경하세요";
    private static final String ACCOUNT_NOT_FOUND_MESSAGE = "아이디가 존재하지 않습니다.";
    private static final String SERVER_ERROR_MESSAGE = "서버 문제 발생 . 다시 시도해 주세요.";

    public CustomAuthenticationFailureHandler(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String errorMessage = "";
        String id = request.getParameter("id");
        log.debug("id : {}", id);

        // 사용자 정보 조회
        Users user = usersRepository.findById(id).orElse(null);
        log.debug("user : {}", user);

        if (user == null) {
            // 사용자 정보가 없으면 아이디 오류 메시지 설정
            errorMessage = ACCOUNT_NOT_FOUND_MESSAGE;
        } else if (exception instanceof BadCredentialsException) {
            log.debug("BadCredentialsException");
            // 비밀번호가 틀리면 실패 횟수 증가
            user.incrementFailedAttempts();
            int failedAttempts = user.getFailedLoginAttempts();
            log.debug("failed login attempts for user {}: {}", id, failedAttempts);

            if (failedAttempts >= 5) {
                // 실패 횟수가 5회를 넘으면 계정 잠금
                user.setAccountLocked(true);
                errorMessage = ACCOUNT_LOCKED_MESSAGE;
            } else {
                // 아이디 또는 비밀번호가 틀린 경우
                errorMessage = INVALID_CREDENTIALS_MESSAGE;
            }

            // 실패한 정보를 저장
            usersRepository.save(user);
        } else if (exception instanceof InternalAuthenticationServiceException) {
            // 서버 오류 처리
            errorMessage = SERVER_ERROR_MESSAGE;
        }

        // 세션에 에러 메시지 저장
        request.getSession().setAttribute("errorMessage", errorMessage);

        // 리다이렉트
        response.sendRedirect("/users/loginP");
    }
}
