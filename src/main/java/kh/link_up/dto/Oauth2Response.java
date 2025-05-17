package kh.link_up.dto;

public interface Oauth2Response {

    String getProvider();
    String getProviderId();
    String getEmail();
    String getName();

    // 추가 필드: 각 소셜 로그인 제공자가 지원하는 필드가 있다면 여기서 처리
    String getPhone();  // 전화번호
    String getBirthyear();  // 생년월일
    String getGender();  // 성별
}

