package kh.link_up.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@Slf4j
public class SessionController {

    /**
     * 세션 만료 시간 연장 (기존 세션 연장)
     *
     * @param request HttpServletRequest
     * @return ResponseEntity
     */
    @PostMapping("/api/extend-session")
    public ResponseEntity<?> extendSession(HttpServletRequest request) {
        log.debug(" extendSession 들어옴");
        HttpSession session = request.getSession(false); // 세션이 존재하는지 확인
        log.debug("extendSession: {}", session);
        if (session != null) {
            session.setMaxInactiveInterval(30 * 60); // 세션 만료 시간 30분으로 연장 (30분 = 1800초)

            // 현재 시간에서 30분 후 계산
            LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(session.getMaxInactiveInterval());

            // 원하는 시간 형식으로 변환
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String formattedExpirationTime = expirationTime.format(formatter);

            log.debug("세션 만료 시간 연장됨, 새로운 만료 시간: {}까지", formattedExpirationTime);
            return ResponseEntity.ok().build();
        } else {
            log.debug("세션이 존재하지 않습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 세션이 없으면 오류 반환
        }
    }

}
