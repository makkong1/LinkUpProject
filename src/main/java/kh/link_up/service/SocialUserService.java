package kh.link_up.service;

import kh.link_up.converter.SocialUserConverter;
import kh.link_up.domain.SocialUser;
import kh.link_up.dto.SocialUserDTO;
import kh.link_up.repository.SocialUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SocialUserService {

    private final SocialUserRepository socialUserRepository;
    private final SocialUserConverter socialUserConverter;

    public SocialUserService(SocialUserRepository socialUserRepository,
            SocialUserConverter socialUserConverter) {
        this.socialUserRepository = socialUserRepository;
        this.socialUserConverter = socialUserConverter;
    }

    public List<SocialUserDTO> getAllSocialUsersFromAdmin() {
        // SocialUser 엔티티 리스트를 가져와서 SocialUserDTO 리스트로 변환
        List<SocialUser> socialUsers = socialUserRepository.findAll();

        // SocialUser 엔티티를 SocialUserDTO로 변환
        return socialUsers.stream()
                .map(socialUserConverter::convertToDTO) // 변환
                .collect(Collectors.toList());
    }

    public Optional<SocialUserDTO> getUserById(Long uIdx) {
        return Optional.ofNullable(socialUserConverter.convertToDTO(socialUserRepository.findById(uIdx).get()));
    }

}
