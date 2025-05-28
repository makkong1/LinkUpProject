package kh.link_up.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Data
@Slf4j
public class CustomOauth2User implements OAuth2User {

    private final Oauth2Response response;
    private final String role;
    private final Long uIdx;  // u_idx 추가
    private final String uNickname;
    private final String uEmail;

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of("email", this.uEmail,"uNickname", this.uNickname);
    }

    //권한 반환
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        return authorities;
    }

    @Override
    public String getName() {
        return response.getName();
    }

    public String getProvider() {
        return response.getProvider();
    }

    public String getNickname() {
        return uNickname;
    }

    public String getUEmail() {
        return uEmail;
    }

    public String getUsername() {
        return response.getProvider() + " " + response.getProviderId();
    }
}
