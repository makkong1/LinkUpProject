package kh.link_up.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kh.link_up.dto.NotionDTO;
import kh.link_up.service.NotionService;
import kh.link_up.service.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUB_ADMIN')")
public class NotionController {

    private final NotionService notionService;

    // 노션 페이지 가져오기
    @GetMapping("/notion/{n_idx}")
    public ResponseEntity<?> getNotion(@PathVariable("n_idx") Long n_idx) {
        NotionDTO notion = notionService.getNotionById(n_idx);
        return new ResponseEntity<>(notion, HttpStatus.OK);
    }

    // 노션 저장
    @PostMapping("/users/{u_idx}/notion")
    public ResponseEntity<?> saveNotion(@PathVariable("u_idx") Long uIdx,
                                        @RequestBody NotionDTO notionDTO) {
        logRequestDetails("saveNotion", uIdx, notionDTO);
        return saveOrUpdateNotion(uIdx, null, notionDTO);  // 저장시 nIdx는 없으므로 null
    }

    @PutMapping("/users/{u_idx}/notion/{n_idx}")
    public ResponseEntity<?> updateNotion(@PathVariable("u_idx") Long uIdx,
                                          @PathVariable("n_idx") Long nIdx,
                                          @RequestBody NotionDTO notionDTO) {
        logRequestDetails("updateNotion", uIdx, notionDTO);
        return saveOrUpdateNotion(uIdx, nIdx, notionDTO);
    }

    @DeleteMapping("/notion/{n_idx}")
    public ResponseEntity<String> deleteNotion(@PathVariable("n_idx") Long nIdx) {
        try {
            // 노션 삭제 서비스 호출
            notionService.deleteNotion(nIdx);

            // 삭제 성공 응답
            return ResponseEntity.ok("노션 삭제 완료");
        } catch (Exception e) {
            // 예외 처리 및 실패 응답
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("노션 삭제 실패: " + e.getMessage());
        }
    }

    // 공통: 노션 저장 또는 수정
    private ResponseEntity<?> saveOrUpdateNotion(Long uIdx, Long nIdx, NotionDTO notionDTO) {
        try {
            notionService.saveNotion(uIdx, nIdx, notionDTO);
            return createResponse(true, "저장 성공!", null, HttpStatus.OK);
        } catch (Exception e) {
            log.error("저장 중 오류 발생: {}", e.getMessage(), e);
            return createResponse(false, null, "저장 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 요청 로깅을 위한 메서드
    private void logRequestDetails(String methodName, Long uIdx, NotionDTO notionDTO) {
        log.info("메소드: {}, 사용자 ID: {}, 노션 제목: {}", methodName, uIdx, notionDTO.getN_title());
    }

    // 응답 생성을 위한 유틸리티 메서드
    private ResponseEntity<?> createResponse(boolean success, String message, String error, HttpStatus status) {
        if (success) {
            return ResponseEntity.status(status).body(message);
        } else {
            return ResponseEntity.status(status).body(error);
        }
    }
}
