package kh.link_up.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@Slf4j
@PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUB_ADMIN')")
@Tag(name = "LinkUp_File", description = "LinkUp 이미지 업로드 및 게시판 파일 관련 API")
public class FileController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final Tika tika = new Tika();

    private static final String[] ALLOWED_IMAGE_TYPES = {"image/gif", "image/jpeg", "image/png"};

    @Operation(summary = "게시글 이미지 파일 조회",
            description = "게시글에 첨부된 이미지 파일을 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "파일 조회 성공", content = @Content(mediaType = "image/*")),
                    @ApiResponse(responseCode = "400", description = "허용되지 않은 파일 형식"),
                    @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음"),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
            })
    @GetMapping("/file/{nickname}/{postTitle}/{filename}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Resource> getImage(@PathVariable String nickname,
                                             @PathVariable String postTitle,
                                             @PathVariable String filename) {

        log.debug("nickname : {}, postTitle : {}, filename : {}", nickname, postTitle, filename);
        String modifiedNickname = "(게시판)" + nickname;
        String filePath = Paths.get(uploadDir, modifiedNickname, postTitle, filename).toString();
        log.info("파일 경로: {}", filePath);

        Path path = Paths.get(filePath);
        FileSystemResource resource = new FileSystemResource(path.toFile());

        if (!resource.exists()) {
            log.error("게시글 파일이 존재하지 않음: {}", filePath);
            return ResponseEntity.notFound().build();
        }

        try {
            String mimeType = tika.detect(resource.getFile());
            if (!isAllowedImageType(mimeType)) {
                log.error("게시글에서 허용되지 않은 파일 형식: {}", mimeType);
                return ResponseEntity.badRequest().body(null);
            }
        } catch (Exception e) {
            log.error("파일 타입 검증 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }

        return ResponseEntity.ok().body(resource);
    }

    private boolean isAllowedImageType(String mimeType) {
        for (String allowedType : ALLOWED_IMAGE_TYPES) {
            if (allowedType.equals(mimeType)) {
                return true;
            }
        }
        return false;
    }

    @Operation(summary = "LinkUp 이미지 업로드",
            description = "LinkUp 이미지 파일을 업로드하고 UUID로 저장합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "업로드 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
                    @ApiResponse(responseCode = "400", description = "허용되지 않은 파일 형식"),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
            })
    @PostMapping("/notion/image")
    public ResponseEntity<String> uploadNotionImage(@RequestParam("file") MultipartFile file) {

        String nickname = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("nickname : {}", nickname);

        String modifiedNickname = "(LinkUp)" + nickname;
        String targetDir = Paths.get(uploadDir, modifiedNickname).toString();

        File dir = new File(targetDir);
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("디렉토리 생성 실패: {}", targetDir);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"디렉토리 생성 실패\"}");
        }

        try {
            String mimeType = tika.detect(file.getInputStream());
            if (!isAllowedImageType(mimeType)) {
                log.error("LinkUp에서 허용되지 않은 파일 형식: {}", mimeType);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"허용되지 않은 파일 형식\"}");
            }
        } catch (IOException e) {
            log.error("파일 검증 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"파일 검증 오류\"}");
        }

        String filename = UUID.randomUUID().toString() + getFileExtension(file.getOriginalFilename());

        try {
            Path filePath = Paths.get(targetDir, filename);
            file.transferTo(filePath.toFile());
            log.info("파일 저장 성공: {}", filePath);

            Map<String, String> result = new HashMap<>();
            result.put("filename", filename);
            String json = new ObjectMapper().writeValueAsString(result);
            return ResponseEntity.ok(json);
        } catch (IOException e) {
            log.error("파일 저장 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"파일 저장 오류\"}");
        }
    }

    @Operation(summary = "LinkUp 이미지 삭제",
            description = "LinkUp 이미지 파일을 삭제합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "삭제 성공"),
                    @ApiResponse(responseCode = "404", description = "파일이 존재하지 않음"),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
            })
    @PostMapping("/notion/delete-image")
    public ResponseEntity<String> deleteNotionImage(
            @Parameter(description = "삭제할 파일 이름", example = "uuid-filename.png") @RequestParam("filename") String filename) {

        String nickname = SecurityContextHolder.getContext().getAuthentication().getName();
        String modifiedNickname = "(LinkUp)" + nickname;
        String targetDir = Paths.get(uploadDir, modifiedNickname).toString();

        Path filePath = Paths.get(targetDir, filename);
        File file = filePath.toFile();

        if (file.exists()) {
            if (file.delete()) {
                log.info("파일 삭제 성공: {}", filePath);
                return ResponseEntity.ok("파일 삭제 성공");
            } else {
                log.error("파일 삭제 실패: {}", filePath);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 삭제 실패");
            }
        } else {
            log.error("파일이 존재하지 않음: {}", filePath);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("파일이 존재하지 않음");
        }
    }

    @Operation(summary = "LinkUp 이미지 조회",
            description = "LinkUp에 저장된 이미지를 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "파일 조회 성공", content = @Content(mediaType = "image/*")),
                    @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음"),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
            })
    @GetMapping("/notion/images/{filename}")
    public ResponseEntity<Resource> getNotionImage(@PathVariable String filename) {

        String nickname = SecurityContextHolder.getContext().getAuthentication().getName();
        String targetDir = Paths.get(uploadDir, "(LinkUp)" + nickname, filename).toString();

        Path path = Paths.get(targetDir);
        FileSystemResource resource = new FileSystemResource(path.toFile());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        try {
            String mimeType = tika.detect(resource.getFile());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex > 0) ? filename.substring(dotIndex) : "";
    }
}
