package kh.link_up.Ouath2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
@Slf4j
public class CustomClientRegistrationRepository {

// OAuth2 클라이언트 등록 정보를 In-Memory에 등록하는 설정 클래스
// 로그인을 위한 설정

    private final SocialClientRegistration socialClientRegistration;

    public CustomClientRegistrationRepository(SocialClientRegistration socialClientRegistration) {

        this.socialClientRegistration = socialClientRegistration;
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {

        ClientRegistration naver = socialClientRegistration.naverClientRegistration();
        ClientRegistration google = socialClientRegistration.googleClientRegistration();

        log.info("✅ Naver ClientRegistration 등록: clientId={}, redirectUri={}",
                naver.getClientId(), naver.getRedirectUri());

        log.info("✅ Google ClientRegistration 등록: clientId={}, redirectUri={}",
                google.getClientId(), google.getRedirectUri());

        return new InMemoryClientRegistrationRepository(socialClientRegistration.naverClientRegistration(), socialClientRegistration.googleClientRegistration());
    }
}
