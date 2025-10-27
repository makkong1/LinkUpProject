package kh.link_up.config;

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

@Component
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UsersRepository usersRepository;
    private static final String ACCOUNT_LOCKED_MESSAGE = "계정이 잠겼습니다. 관리자에게 문의하거나 비밀번로를 변경하세요";

    // 생성자로 OAuth2AuthorizedClientService와 UsersRepository를 주입받습니다.
    public CustomAuthenticationSuccessHandler(OAuth2AuthorizedClientService authorizedClientService,
            UsersRepository usersRepository) {
        this.authorizedClientService = authorizedClientService;
        this.usersRepository = usersRepository;
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
                    oauth2Authentication.getName());

            if (authorizedClient != null) {
                // OAuth2AccessToken을 가져옵니다.
                OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
                log.debug("Oauth2 Access token: {}", accessToken.getTokenValue());
                if (accessToken != null) {
                    // Access Token 로그 찍기
                    log.info("Access Token: {}", accessToken.getTokenValue());
                }
            } else {
                log.warn("No OAuth2AuthorizedClient found.");
            }
        }

        // 사용자의 역할에 따라 리다이렉트 URL 설정
        if (role.contains("ROLE_ADMIN") || role.contains("ROLE_SUB_ADMIN") || role.contains("ADMIN")
                || role.contains("SUB_ADMIN")) {
            response.sendRedirect("/admin"); // 관리자 대시보드로 리다이렉트
        } else if (role.contains("USER") || role.contains("ROLE_USER")) {
            response.sendRedirect("/board"); // 사용자 대시보드로 리다이렉트
        }

        if (user == null) {
            // DB에 없는 사용자 → 신규 OAuth2 유저면 자동 회원가입
            // OAuth2에서 이메일 정보 가져오기
            String email = "";
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
                email = oauth2Token.getPrincipal().getAttribute("email");
                if (email == null) {
                    email = username; // 이메일이 없으면 username을 사용
                }
            } else {
                email = username; // 일반 로그인의 경우 username을 사용
            }

            user = Users.builder()
                    .id(username)
                    .uEmail(email)
                    .uRole("USER")
                    .uUsername(username)
                    .uNickname(username)
                    .password("") // OAuth2 사용자는 비밀번호 없음
                    .failedLoginAttempts(0)
                    .accountLocked(false)
                    .build();
            usersRepository.save(user);
            log.info("신규 OAuth2 사용자 생성: {}", username);
        }

        // 로그인 성공 시 사용자 정보 업데이트: 로그인 실패 횟수 초기화 및 계정 잠금 해제
        if (user != null && user.isAccountLocked()) {
            // 로그인 실패 횟수 초기화
            user.setFailedLoginAttempts(0);
            // 계정 잠금 해제
            user.setAccountLocked(false);

            // 변경된 사용자 정보 저장
            usersRepository.save(user);

            log.debug("로그인성공 시도횟수 및 잠금상태 초기화 / 사용자: {}, 잠금상태: {}, 실패횟수: {}",
                    username, user.isAccountLocked(), user.getFailedLoginAttempts());
        }
    }
}
