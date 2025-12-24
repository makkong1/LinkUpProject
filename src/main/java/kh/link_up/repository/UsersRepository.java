package kh.link_up.repository;

import kh.link_up.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface UsersRepository extends JpaRepository<Users, Long> {

    List<Users> findAll();

    Optional<Users> findById(String id);

    boolean existsById(String u_id);

    boolean existsByuNickname(String nickname); // 닉네임 중복 확인용

    Users findByuUsername(String username);

    Users findByuNickname(String nickname);

    Optional<Users> findByuEmail(String email);
}
