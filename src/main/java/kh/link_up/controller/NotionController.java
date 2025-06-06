package kh.link_up.controller;

import kh.link_up.dto.NotionDTO;
import kh.link_up.service.NotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUB_ADMIN')")
@Tag(name = "NotionController", description = "LinkUp 페이지 CRUD API")
public class NotionController {

    private final NotionService notionService;

    @Operation(summary = "LinkUp 페이지 조회",
            description = "n_idx를 기반으로 LinkUp 페이지를 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "해당 LinkUp 페이지 없음"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    @GetMapping("/notion/{n_idx}")
    public ResponseEntity<?> getNotion(
            @Parameter(description = "LinkUp 페이지 식별자", example = "1") @PathVariable("n_idx") Long n_idx) {
        NotionDTO notion = notionService.getNotionById(n_idx);
        return new ResponseEntity<>(notion, HttpStatus.OK);
    }

    @Operation(summary = "LinkUp 페이지 저장",
            description = "사용자 u_idx에 해당하는 LinkUp 페이지를 새로 저장합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "저장 성공"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    @PostMapping("/users/{u_idx}/notion")
    public ResponseEntity<?> saveNotion(
            @Parameter(description = "사용자 식별자", example = "1") @PathVariable("u_idx") Long uIdx,
            @Parameter(description = "저장할 LinkUp 데이터") @RequestBody NotionDTO notionDTO) {
        logRequestDetails("saveNotion", uIdx, notionDTO);
        return saveOrUpdateNotion(uIdx, null, notionDTO);  // 저장시 nIdx는 없으므로 null
    }

    @Operation(summary = "LinkUp 페이지 수정",
            description = "사용자 u_idx와 LinkUp n_idx를 기반으로 LinkUp 페이지를 수정합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 성공"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    @PutMapping("/users/{u_idx}/notion/{n_idx}")
    public ResponseEntity<?> updateNotion(
            @Parameter(description = "사용자 식별자", example = "1") @PathVariable("u_idx") Long uIdx,
            @Parameter(description = "LinkUp 페이지 식별자", example = "1") @PathVariable("n_idx") Long nIdx,
            @Parameter(description = "수정할 LinkUp 데이터") @RequestBody NotionDTO notionDTO) {
        logRequestDetails("updateNotion", uIdx, notionDTO);
        return saveOrUpdateNotion(uIdx, nIdx, notionDTO);
    }

    @Operation(summary = "LinkUp 페이지 삭제",
            description = "LinkUp 페이지 n_idx를 기반으로 삭제합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "삭제 성공"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    @DeleteMapping("/notion/{n_idx}")
    public ResponseEntity<String> deleteNotion(
            @Parameter(description = "LinkUp 페이지 식별자", example = "1") @PathVariable("n_idx") Long nIdx) {
        try {
            notionService.deleteNotion(nIdx);
            return ResponseEntity.ok("LinkUp 삭제 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("LinkUp 삭제 실패: " + e.getMessage());
        }
    }

    // 공통: 저장 또는 수정 처리
    private ResponseEntity<?> saveOrUpdateNotion(Long uIdx, Long nIdx, NotionDTO notionDTO) {
        try {
            notionService.saveNotion(uIdx, nIdx, notionDTO);
            return createResponse(true, "저장 성공!", null, HttpStatus.OK);
        } catch (Exception e) {
            log.error("저장 중 오류 발생: {}", e.getMessage(), e);
            return createResponse(false, null, "저장 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 요청 로깅
    private void logRequestDetails(String methodName, Long uIdx, NotionDTO notionDTO) {
        log.info("메소드: {}, 사용자 ID: {}, LinkUp 제목: {}", methodName, uIdx, notionDTO.getN_title());
    }

    // 응답 생성 유틸
    private ResponseEntity<?> createResponse(boolean success, String message, String error, HttpStatus status) {
        if (success) {
            return ResponseEntity.status(status).body(message);
        } else {
            return ResponseEntity.status(status).body(error);
        }
    }
}
