package kh.link_up.service;

import kh.link_up.domain.Users;
import kh.link_up.dto.CustomUserDetails;
import kh.link_up.repository.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    //AuthenticationManager가 이 클래스의 loadUserByUsername메서드를 호출
    private final UsersRepository usersRepository;

    public CustomUserDetailsService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
        log.debug("customUserDetailsService 들어왔다");
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        System.out.println("loadUserByUsername 하이");
        log.debug("id : {}",id);
        // 사용자 조회 (Optional로 처리)
        Optional<Users> optionalUser = usersRepository.findById(id);

        // 사용자가 없으면 예외 처리
        if (optionalUser.isEmpty()) {
            throw new UsernameNotFoundException(id + "는 없는 유저입니다.");
        }

        // 사용자 정보 가져오기
        Users user = optionalUser.get();
        log.debug("사용자 정보 조회 성공: {}", user);

        return new CustomUserDetails(user); // CustomUserDetails를 반환
    }
}
