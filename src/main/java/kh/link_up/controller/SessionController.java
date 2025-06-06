package kh.link_up.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@Slf4j
@Tag(name = "Session", description = "세션 관련 API")
public class SessionController {

    /**
     * 세션 만료 시간 연장 (기존 세션 연장)
     *
     * @param request HttpServletRequest
     * @return ResponseEntity
     */
    @Operation(summary = "세션 만료 시간 연장",
            description = "현재 존재하는 세션의 만료 시간을 30분 연장합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "세션 만료 시간 연장 성공"),
                    @ApiResponse(responseCode = "401", description = "세션이 존재하지 않아 연장 실패")
            })
    @PostMapping("/api/extend-session")
    public ResponseEntity<?> extendSession(HttpServletRequest request) {
        log.debug("extendSession 호출됨");
        HttpSession session = request.getSession(false); // 세션 존재 확인
        log.debug("extendSession: {}", session);
        if (session != null) {
            session.setMaxInactiveInterval(30 * 60); // 30분 연장

            LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(session.getMaxInactiveInterval());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String formattedExpirationTime = expirationTime.format(formatter);

            log.debug("세션 만료 시간 연장됨, 새로운 만료 시간: {}까지", formattedExpirationTime);
            return ResponseEntity.ok().build();
        } else {
            log.debug("세션이 존재하지 않습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
