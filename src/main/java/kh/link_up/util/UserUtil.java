package kh.link_up.util;

import kh.link_up.domain.Users;
import kh.link_up.service.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserUtil {

    private final UsersService usersService;

    public String getUserNickname(Principal principal) {
        log.info("사용자 닉네임 찾기 시작");
        String nickname = "";

        if (principal instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2User oauth2User = oauth2Token.getPrincipal();
            log.debug("UserUtil - 소셜 로그인 사용자: {}", oauth2User);
            nickname = oauth2User.getAttribute("uNickname");
        } else {
            Users user = usersService.getUserByNickname(principal.getName());
            nickname = user.getUNickname();
        }

        log.info("닉네임찾음 : {}", nickname);
        return nickname;
    }


    public String getUserIdentifier(Principal principal, Authentication authentication) {
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            return principal.getName(); // 닉네임
        } else if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            OAuth2User oauth2User = oauth2Authentication.getPrincipal();
            return oauth2User.getAttribute("email"); // 이메일
        }
        return null;
    }
}
