package kh.link_up.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Service
@Slf4j
public class CommentNotificationSubscriber {

    private final Map<String, SseEmitter> clientConnections = new ConcurrentHashMap<>();

    // 클라이언트 연결 추가
    public void addClientConnection(String userId, SseEmitter emitter) {
        if (clientConnections.containsKey(userId)) {
            log.debug("이미 연결된 userId: {}", userId);
        }
        clientConnections.put(userId, emitter);
        log.debug("클라이언트 연결 추가: {}", userId);
    }

    // 클라이언트 연결 제거
    public void removeClientConnection(String userId) {
        clientConnections.remove(userId);
        log.debug("클라이언트 연결 제거: {}", userId);
    }

    // Redis에서 메시지를 받으면 이 메서드가 호출됨
    public void handleMessage(String message) {
        log.info("clientConnections:{}", clientConnections);
        log.debug("현재 clientConnections 상태: {}", clientConnections.keySet());
        log.debug("Redis에서 받은 메시지: {}", message);

        String[] parts = message.split(":", 2);

        if (parts.length < 2) {
            log.error("잘못된 메시지 형식: {}", message);
            return;
        }

        String userId = parts[0].trim();
        String notificationMessage = parts[1].trim();

        log.info("userId: {}, notificationMessage: {}", userId, notificationMessage);

        SseEmitter emitter = clientConnections.get(userId);
        log.debug("emitter user: {}", emitter);

        if (emitter != null) {
            try {
                emitter.send(notificationMessage);
                log.debug("메시지 전송: {}", notificationMessage);
            } catch (Exception e) {
                emitter.completeWithError(e);
                clientConnections.remove(userId);
            }
        } else {
            log.debug("해당 userId에 대한 emitter가 존재하지 않음: {}", userId);
        }
    }

}



