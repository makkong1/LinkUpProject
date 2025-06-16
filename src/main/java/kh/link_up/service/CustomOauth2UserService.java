package kh.link_up.service;

import kh.link_up.domain.SocialUser;
import kh.link_up.domain.Users;
import kh.link_up.dto.CustomOauth2User;
import kh.link_up.dto.GoogleResponse;
import kh.link_up.dto.NaverResponse;
import kh.link_up.dto.Oauth2Response;
import kh.link_up.repository.SocialUserRepository;
import kh.link_up.repository.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class CustomOauth2UserService extends DefaultOAuth2UserService {

    private final UsersRepository usersRepository;
    private final SocialUserRepository socialUserRepository; // social_user 테이블 관리하는 repository 추가

    public CustomOauth2UserService(UsersRepository usersRepository, SocialUserRepository socialUserRepository) {
        this.usersRepository = usersRepository;
        this.socialUserRepository = socialUserRepository;
        log.debug("CustomOauth2UserService 생성");
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        log.debug("loadUser 들어옴");

        // OAuth2User 정보를 로드합니다.
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.debug("oAuth2User: {}", oAuth2User);

        // 소셜 로그인 제공자를 확인합니다.
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.debug("social registrationId: {}", registrationId);

        Oauth2Response oAuth2Response = null;

        // 소셜 로그인 제공자에 따른 response 객체 생성
        if (registrationId.equals("naver")) {
            oAuth2Response = new NaverResponse(oAuth2User.getAttributes());
            log.debug("naver oAuth2Response :{}", oAuth2Response);
        } else if (registrationId.equals("google")) {
            oAuth2Response = new GoogleResponse(oAuth2User.getAttributes());
            log.debug("google oAuth2Response :{}", oAuth2Response);
        } else {
            return null; // 알 수 없는 제공자 처리
        }

        // 소셜 로그인 제공자와 ID를 합쳐서 유저의 고유 ID를 만듭니다.
        String socialId = oAuth2Response.getProvider() + " " + oAuth2Response.getProviderId();
        String email = oAuth2Response.getEmail();
        String name = oAuth2Response.getName();
        log.debug("socialId: {}", socialId);
        log.debug("socialEmail: {}", email);
        log.debug("socialName: {}", name);

        // 소셜 사용자 정보를 `social_user` 테이블에 저장하거나 업데이트합니다.
        Optional<SocialUser> existingSocialUser = socialUserRepository
                .findByProviderAndProviderUserId(oAuth2Response.getProvider(), oAuth2Response.getProviderId());
        SocialUser socialUser;

        if (existingSocialUser.isEmpty()) {
            System.out.println("existingSocialUser 없어서 들어옴");
            // 소셜 유저가 없다면 새로 생성
            socialUser = new SocialUser();
            socialUser.setProvider(oAuth2Response.getProvider());
            socialUser.setProviderUserId(oAuth2Response.getProviderId());
            socialUser.setEmail(email);
            socialUser.setName(name);
            log.debug("socialUserEmail: {}", email);
            Optional<Users> user = usersRepository.findByuEmail(email);
            if (user.isPresent()) {
                socialUser.setUser(user.get()); // Users 객체를 설정
                socialUser.setUserId(user.get().getUIdx()); // user_id 컬럼에 u_idx 값을 설정
            }
            log.debug("socialUserIdx: {}", socialUser.getUser());
            socialUser.setPhone(oAuth2Response.getPhone() != null ? oAuth2Response.getPhone() : "");
            socialUser.setBirthyear(oAuth2Response.getBirthyear() != null ? oAuth2Response.getBirthyear() : "");
            socialUser.setGender(oAuth2Response.getGender() != null ? oAuth2Response.getGender() : "");

            // 새로운 소셜 유저를 데이터베이스에 저장
            socialUserRepository.save(socialUser);
        } else {
            System.out.println("existingSocialUser 있어서 들어옴");
            // 기존 소셜 유저 정보 업데이트
            socialUser = existingSocialUser.get();
            socialUser.setEmail(email); // 이메일 업데이트
            socialUser.setName(name); // 이름 업데이트
            socialUser.setPhone(oAuth2Response.getPhone() != null ? oAuth2Response.getPhone() : socialUser.getPhone());
            socialUser.setBirthyear(
                    oAuth2Response.getBirthyear() != null ? oAuth2Response.getBirthyear() : socialUser.getBirthyear());
            socialUser.setGender(
                    oAuth2Response.getGender() != null ? oAuth2Response.getGender() : socialUser.getGender());

            // 업데이트된 소셜 유저 정보를 저장
            socialUserRepository.save(socialUser);
        }

        String role = "USER"; // 기본적으로 `ROLE_USER`를 할당

        // CustomOAuth2User를 사용하여 OAuth2User 반환
        return new CustomOauth2User(oAuth2Response, role, socialUser.getSocialUserId(), socialUser.getName(), socialUser.getEmail());
    }
}
