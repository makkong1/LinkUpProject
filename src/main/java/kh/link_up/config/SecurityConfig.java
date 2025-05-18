package kh.link_up.config;

import kh.link_up.Ouath2.CustomClientRegistrationRepository;
import kh.link_up.Ouath2.CustomOAuth2AuthorizedClientService;
import kh.link_up.service.CustomOauth2UserService;
import kh.link_up.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;
    private final CustomLogoutSuccessHandler logoutSuccessHandler; // 로그아웃 성공 핸들러
    private final CustomOauth2UserService oauth2UserService;
    private final CustomClientRegistrationRepository oauth2ClientRegistrationRepository;
    private final CustomOAuth2AuthorizedClientService oauth2AuthorizedClientService;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                // 이거는 권한이나 인증없이 접근을 할 수 있는 페이지들, 메서드매핑인거지
                                "/users/login",
                                "/users/loginP",
                                "/users/newP",
                                "/users/new/checkId",
                                "/users/new/checkNickname",
                                "/",
                                "/login",
                                "/login/**",
                                "/users",
                                "/users/new",
                                "/users/findPwdP",
                                "/users/findPassword",
                                "/users/verifyAuthCode",
                                "/users/changePassword",
                                "/board",
                                "/board/{bIdx:\\d+}",
                                "/users/logout",
                                "/oauth2/**",
                                "/file/**",
                                "/notion/image", // 노션파일
                                "/js/**",
                                "/actuator/**",
                                "/monitoring/**",
                                "/err/**",
                                "/favicon.ico"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUB_ADMIN")
                        //모니터링 관련 부분은 개발자 로컬환경에서만 가능하게 만들기
                        .requestMatchers("/actuator/**", "/monitoring/**").access((authentication, context) -> {
                            String ip = context.getRequest().getRemoteAddr();
                            return new AuthorizationDecision(ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1"));
                        })
                        .anyRequest().authenticated())

                // 소셜 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        // 로그인 페이지 경로 설정
                        .loginPage("/users/loginP")
                        // OAuth2 클라이언트 등록 정보를 제공하는 리포지토리 설정
                        .clientRegistrationRepository(oauth2ClientRegistrationRepository.clientRegistrationRepository())
                        // OAuth2 클라이언트 인증 정보를 저장하고 관리하는 서비스
                        .authorizedClientService(oauth2AuthorizedClientService.authorizedClientService(jdbcTemplate, oauth2ClientRegistrationRepository.clientRegistrationRepository()))
                        // 소셜 로그인에서 인증 후, 사용자 정보를 가져오는 엔드포인트를 설정하는 부분
                        .userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig.userService(oauth2UserService))
                        .successHandler(successHandler) // 로그인 성공 시 처리
                        .failureHandler(failureHandler) // 로그인 실패 시 처리
                        .permitAll())

                .formLogin(auth -> auth
                        .usernameParameter("id") // 사용자 ID 필드
                        .passwordParameter("password") // 비밀번호 필드
                        .loginPage("/users/loginP") // 로그인 페이지 경로 설정
                        .loginProcessingUrl("/login") // 로그인 처리 URL
                        .successHandler(successHandler) // 로그인 성공 시 처리
                        .failureHandler(failureHandler) // 로그인 실패 시 처리
                )

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/monitoring/**", "/actuator/**") // ✅ CSRF도 무시해줘야 등록 가능
                )

                .httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 인증 비활성화
                .logout(logout -> logout
                        .logoutUrl("/users/logout") // 로그아웃 URL 설정
                        .invalidateHttpSession(true) // 세션 무효화
                        .deleteCookies("JSESSIONID") // 세션 쿠키 삭제
                        .logoutSuccessHandler(logoutSuccessHandler) // 로그아웃 성공 후 핸들러 설정
                )

                .sessionManagement(session -> session
                        .sessionFixation().migrateSession() // 세션공격 방어
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // 세션 필요 시 생성
                        .maximumSessions(1) // 동일 사용자 세션 1개 제한
                        .expiredSessionStrategy(new CustomSessionExpiredStrategy()))

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                // 인증되지 사용자 로그인페이지로 리다이렉트트
                                new CustomAuthenticationEntryPoint())
                        .accessDeniedHandler(new CustomAccessDeniedHandler()));
        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService customUserDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
