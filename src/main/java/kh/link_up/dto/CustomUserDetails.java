package kh.link_up.dto;

import kh.link_up.domain.Users;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

@Slf4j
public record CustomUserDetails(Users user) implements UserDetails, Serializable {

    public CustomUserDetails(Users user) {
        this.user = user;
        // 사용자 역할을 권한으로 매핑하여 미리 초기화
        log.debug("CustomUserDetails 들어왔다 : {}", this.user);
    }

    @Override //사용자 조회
    public Collection<? extends GrantedAuthority> getAuthorities() {
        System.out.println("getAuthorities 들어옴");
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        String role = user.getURole().toUpperCase();
        log.debug("role : {}", role);

        // "ROLE_"이 이미 포함되어 있으면, 중복을 방지하기 위해 "ROLE_"만 추가
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;  // "ROLE_"이 없다면 붙여서 처리
        }

        authorities.add(new SimpleGrantedAuthority(role)); // 권한 문자열 반환
        log.debug(authorities.toString());
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword(); // 사용자 비밀번호 반환
    }

    @Override
    public String getUsername() {
        return user.getId(); // 사용자 ID 반환
    }

    public String getU_nickname() {
        return user.getUNickname();
    }

    public String getU_id() {
        return user.getId();
    }

    public Long getU_idx() {
        return user.getUIdx();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 여부 (true: 만료되지 않음)
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.isAccountLocked(); // 계정 잠금 여부 (true: 잠금되지 않음)
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 자격 증명 만료 여부 (true: 만료되지 않음)
    }

    @Override
    public boolean isEnabled() {
        return true; // 계정 활성화 여부 (true: 활성화됨)
    }

}
