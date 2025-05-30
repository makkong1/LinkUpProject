package kh.link_up.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
public class FileController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    // 이 컨트롤러는 노션에서 Ckeditor에 이미지를 업로드하면 자동으로 요청이 보내져서
    // 그래서 이 클래스를 만듬 
    // 보드게시글 파일업로드랑은 상관없음 파일업로드라고 해봐야 이미지업로드가 끝이지만
    
    // Tika 객체를 생성하여 파일 유형을 검증
    // Tika: 파일의 MIME 타입을 분석하여 악성 파일을 식별할 수 있도록 돕는 라이브러리.
    private Tika tika = new Tika();

    // 이미지 확장자 목록 (gif, jpg, png)
    private static final String[] ALLOWED_IMAGE_TYPES = {"image/gif", "image/jpeg", "image/png"};

    // 게시판 파일을 불러오는 API. 파일의 확장자와 실제 내용을 Tika 라이브러리로 검사하여 보안 공격을 방어
    @GetMapping("/file/{nickname}/{postTitle}/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String nickname,
                                             @PathVariable String postTitle,
                                             @PathVariable String filename) {
        log.debug("nickname : {}, postTitle : {}, filename : {}", nickname, postTitle, filename);
        // 파일 경로
        String modifiedNickname = "(게시판)" + nickname;
        String filePath = Paths.get(uploadDir, modifiedNickname, postTitle, filename).toString();
        log.info("파일 경로: {}", filePath);  // 실제 파일 경로를 로그로 확인

        // 파일 경로로 실제 파일을 가져옴
        Path path = Paths.get(filePath);
        FileSystemResource resource = new FileSystemResource(path.toFile());

        // 파일이 존재하지 않으면 404 반환
        if (!resource.exists()) {
            log.error("게시글 파일이 존재하지 않음: {}", filePath); // 파일이 없을 경우 경로 확인
            return ResponseEntity.notFound().build();
        }

        // MIME 타입을 확인하여 이미지 형식이 gif, jpg, png인지 확인
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

        log.debug("파일 정상 처리: {}", ResponseEntity.ok().body(resource));
        return ResponseEntity.ok().body(resource);
    }

    // 허용된 이미지 타입인지 확인하는 메소드
    private boolean isAllowedImageType(String mimeType) {
        for (String allowedType : ALLOWED_IMAGE_TYPES) {
            if (allowedType.equals(mimeType)) {
                return true;
            }
        }
        return false;
    }

    // 노션 실제이미지 저장
    @PostMapping("/notion/image")
    public ResponseEntity<String> uploadNotionImage(@RequestParam("file") MultipartFile file) {
        // 사용자 닉네임 가져오기
        String nickname = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("nickname : {}", nickname);

        // 닉네임을 "(노션)"으로 수정한 후 저장 디렉토리 경로 설정
        String modifiedNickname = "(노션)" + nickname;
        String targetDir = Paths.get(uploadDir, modifiedNickname).toString();

        // 디렉토리 생성 (없으면 생성)
        File dir = new File(targetDir);
        if (!dir.exists()) {
            boolean dirCreated = dir.mkdirs();
            if (!dirCreated) {
                log.error("디렉토리 생성 실패: {}", targetDir);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"디렉토리 생성 실패\"}");
            }
        }

        // 파일 확장자 및 MIME 타입 검증
        try {
            String mimeType = tika.detect(file.getInputStream()); // Tika 라이브러리로 MIME 타입 확인
            if (!isAllowedImageType(mimeType)) {  // 허용된 이미지 타입인지 확인
                log.error("노션에서 허용되지 않은 파일 형식: {}", mimeType);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"허용되지 않은 파일 형식\"}");
            }
        } catch (IOException e) {
            log.error("파일 검증 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"파일 검증 오류\"}");
        }

        // 파일 이름을 UUID로 변경하여 저장 (중복 방지)
        String filename = UUID.randomUUID().toString() + getFileExtension(file.getOriginalFilename());

        // 파일 저장
        try {
            Path filePath = Paths.get(targetDir, filename);
            file.transferTo(filePath.toFile());
            log.info("파일 저장 성공: {}", filePath);

            // JSON 형식으로 파일명을 반환 (예: {"filename":"abc123.jpg"})
            Map<String, String> result = new HashMap<>();
            result.put("filename", filename);
            String json = new ObjectMapper().writeValueAsString(result);
            return ResponseEntity.ok(json);
        } catch (IOException e) {
            log.error("파일 저장 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"파일 저장 오류\"}");
        }
    }

    @PostMapping("/notion/delete-image")
    public ResponseEntity<String> deleteNotionImage(@RequestParam("filename") String filename) {
        String nickname = SecurityContextHolder.getContext().getAuthentication().getName();
        String modifiedNickname = "(노션)" + nickname;
        String targetDir = Paths.get(uploadDir, modifiedNickname).toString();

        // 파일 경로 설정
        Path filePath = Paths.get(targetDir, filename);

        // 파일이 존재하는지 확인하고 삭제
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

    // 노션이미지 파일반환
    @GetMapping("/notion/images/{filename}")
    public ResponseEntity<Resource> getNotionImage(@PathVariable String filename) {
        // 파일 경로 설정 (유저 닉네임, 파일명 동적 처리)
        String nickname = SecurityContextHolder.getContext().getAuthentication().getName();
        String targetDir = Paths.get("D:/LinkUpFileFolder", "(노션)" + nickname, filename).toString();

        // 파일 경로를 이용하여 파일 반환
        Path path = Paths.get(targetDir);
        FileSystemResource resource = new FileSystemResource(path.toFile());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();  // 파일이 존재하지 않으면 404
        }

        // MIME 타입 확인 후 응답
        try {
            String mimeType = tika.detect(resource.getFile());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // 파일 확장자 추출 메소드
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex > 0) ? filename.substring(dotIndex) : "";
    }
}


