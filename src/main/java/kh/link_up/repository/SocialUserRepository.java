package kh.link_up.repository;

import kh.link_up.domain.SocialUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialUserRepository extends JpaRepository<SocialUser, Long> {

    Optional<SocialUser> findByProviderAndProviderUserId(String provider, String providerId);
    SocialUser findByName(String providerUserId);
    SocialUser findByEmail(String email);
}
