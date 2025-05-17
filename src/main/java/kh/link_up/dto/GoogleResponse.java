package kh.link_up.dto;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class GoogleResponse implements Oauth2Response {
    private Map<String, Object> attributes;

    public GoogleResponse(Map<String, Object> attributes) {
        this.attributes = attributes;
        log.debug("Googler Response: {}", attributes);
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getPhone() {
        return null;  // 구글은 기본적으로 전화번호 제공하지 않음
    }

    @Override
    public String getBirthyear() {
        return null;  // 구글은 생년월일을 제공하지 않음
    }

    @Override
    public String getGender() {
        return null;  // 구글은 성별을 제공하지 않음
    }
}

