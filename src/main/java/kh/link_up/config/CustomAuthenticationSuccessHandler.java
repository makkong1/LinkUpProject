package kh.link_up.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kh.link_up.domain.Users;
import kh.link_up.repository.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UsersRepository usersRepository;
    private static final String ACCOUNT_LOCKED_MESSAGE = "계정이 잠겼습니다. 관리자에게 문의하거나 비밀번로를 변경하세요";

    // 생성자로 OAuth2AuthorizedClientService와 UsersRepository를 주입받습니다.
    public CustomAuthenticationSuccessHandler(OAuth2AuthorizedClientService authorizedClientService, UsersRepository usersRepository) {
        this.authorizedClientService = authorizedClientService;
        this.usersRepository = usersRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain, Authentication authentication) throws IOException, ServletException {
        // 인증 성공시 처리
        onAuthenticationSuccess(request, response, authentication);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // 인증된 사용자의 정보 가져오기
        String errorMessage = "";
        String username = authentication.getName();
        String role = authentication.getAuthorities().toString();
        log.debug("username: {}", username);
        log.debug("role: {}", role);
        log.debug("인증된 사용자 세션정보 : {}", SecurityContextHolder.getContext().getAuthentication().getName());
        // 사용자 정보 조회
        Users user = usersRepository.findById(username).orElse(null);
        log.info("user lock : {}", user != null ? user.isAccountLocked() : "null");

        // 계정이 잠금 상태인 경우 로그인 페이지로 리다이렉트
        if (user != null && user.isAccountLocked()) {
            log.debug("Account is locked for user: {}", username);
            errorMessage = ACCOUNT_LOCKED_MESSAGE;
            request.getSession().setAttribute("errorMessage", errorMessage);
            response.sendRedirect("/users/loginP"); // 로그인 페이지로 리다이렉트
            return;
        }
        // 세션에서 에러 메시지 제거
        request.getSession().removeAttribute("errorMessage");

        // 인증된 사용자가 OAuth2 로그인인 경우
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauth2Authentication = (OAuth2AuthenticationToken) authentication;

            // OAuth2AuthorizedClient 가져오기
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                    oauth2Authentication.getAuthorizedClientRegistrationId(),
                    oauth2Authentication.getName()
            );

            if (authorizedClient != null) {
                // OAuth2AccessToken을 가져옵니다.
                OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
                log.debug("Access token: {}", accessToken.getTokenValue());
                if (accessToken != null) {
                    // Access Token 로그 찍기
                    log.info("Access Token: {}", accessToken.getTokenValue());
                } else {
                    log.warn("No Access Token found.");
                }
            } else {
                log.warn("No OAuth2AuthorizedClient found.");
            }
        }

        // 사용자의 역할에 따라 리다이렉트 URL 설정
        if (role.contains("ROLE_ADMIN") || role.contains("ROLE_SUB_ADMIN") || role.contains("ADMIN") || role.contains("SUB_ADMIN")) {
            response.sendRedirect("/admin");  // 관리자 대시보드로 리다이렉트
        } else if (role.contains("USER") || role.contains("ROLE_USER")) {
            response.sendRedirect("/board");  // 사용자 대시보드로 리다이렉트
        }

        // 로그인 성공 시 사용자 정보 업데이트: 로그인 실패 횟수 초기화 및 계정 잠금 해제
        Optional<Users> optionalUser = usersRepository.findById(username);
        log.debug("User: {}", optionalUser);
        if (optionalUser.isPresent()) {
            Users userToUpdate = optionalUser.get();

            // 로그인 실패 횟수 초기화
            userToUpdate.setFailedLoginAttempts(0);
            // 계정 잠금 해제
            userToUpdate.setAccountLocked(false);

            // 변경된 사용자 정보 저장
            usersRepository.save(userToUpdate);

            log.debug("로그인성공 시도횟수 및 잠금상태 초기화 /  {}. ", username);
        } else {
            log.warn("유저 없음: {}", username);
        }
    }
}
