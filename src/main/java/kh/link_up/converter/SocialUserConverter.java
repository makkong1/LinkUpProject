package kh.link_up.converter;

import kh.link_up.domain.SocialUser;
import kh.link_up.dto.SocialUserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SocialUserConverter implements EntitiyConverter<SocialUser, SocialUserDTO> {

    @Override
    public SocialUserDTO convertToDTO(SocialUser entity) {
        if (entity == null) {
            return null;
        }

        // SocialUser 엔티티를 SocialUserDTO로 변환
        return SocialUserDTO.builder()
                .socialUserId(entity.getSocialUserId())
                .provider(entity.getProvider())
                .providerUserId(entity.getProviderUserId())
                .email(entity.getEmail())
                .name(entity.getName())
                .profileImageUrl(entity.getProfileImageUrl())
                .phone(entity.getPhone())
                .birthyear(entity.getBirthyear())
                .gender(entity.getGender())
                .userId(entity.getUser() != null ? Long.valueOf(entity.getUser().getUIdx()) : null)  // 연관된 Users의 ID를 가져옵니다.
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .users(entity.getUser())
                .board(entity.getBoard())
                .comments(entity.getComments())
                .notion(entity.getNotions())
                .build();
    }

    @Override
    public SocialUser convertToEntity(SocialUserDTO dto) {
        if (dto == null) {
            return null;
        }

        // SocialUserDTO를 SocialUser 엔티티로 변환
        SocialUser socialUser = new SocialUser();
        socialUser.setSocialUserId(dto.getSocialUserId());
        socialUser.setProvider(dto.getProvider());
        socialUser.setProviderUserId(dto.getProviderUserId());
        socialUser.setEmail(dto.getEmail());
        socialUser.setName(dto.getName());
        socialUser.setProfileImageUrl(dto.getProfileImageUrl());
        socialUser.setPhone(dto.getPhone());
        socialUser.setBirthyear(dto.getBirthyear());
        socialUser.setGender(dto.getGender());
        socialUser.setUser(dto.getUsers());

        // userId가 null이 아니면 해당 User를 찾아서 설정
        if (dto.getUserId() != null) {
            // 실제로 User 엔티티를 찾아서 설정해야 합니다.
            // 예를 들어, userRepository.findById(dto.getUserId()).orElse(null)처럼 처리합니다.
            // socialUser.setUser(userRepository.findById(dto.getUserId()).orElse(null));
            log.debug("몰라 진짜 나도!!! 하다보니까 복잡해!!! 이게 복잡하면 안되는데;;");
        }

        socialUser.setCreatedAt(dto.getCreatedAt());
        socialUser.setUpdatedAt(dto.getUpdatedAt());
        socialUser.getComments();
        socialUser.getNotions();
        socialUser.getBoard();


        return socialUser;
    }
}
