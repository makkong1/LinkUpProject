package kh.link_up.dto;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class NaverResponse implements Oauth2Response {
    private Map<String, Object> attributes;

    public NaverResponse(Map<String, Object> response) {
        Object res = response.get("response");
        if (res instanceof Map<?, ?> map) {
            // 타입 안전하게 변환
            this.attributes = (Map<String, Object>) map;
            log.debug("Naver Response attributes: {}", attributes);
        } else {
            this.attributes = Map.of(); // 빈 맵으로 초기화
            log.warn("Unexpected response format from Naver: {}", response);
        }
    }

    @Override
    public String getProvider() {
        return "naver";
    }

    @Override
    public String getProviderId() {
        return attributes.get("id").toString();
    }

    @Override
    public String getEmail() {
        return attributes.get("email").toString();
    }

    @Override
    public String getName() {
        return attributes.get("name").toString();
    }

    @Override
    public String getPhone() {
        return attributes.get("mobile").toString(); // 네이버에서 제공하는 전화번호
    }

    @Override
    public String getBirthyear() {
        return attributes.get("birthyear").toString(); // 네이버에서 제공하는 생년월일
    }

    @Override
    public String getGender() {
        return attributes.get("gender").toString(); // 네이버에서 제공하는 성별
    }
}
