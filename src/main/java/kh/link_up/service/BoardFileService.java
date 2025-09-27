package kh.link_up.service;

import kh.link_up.domain.Board;
import kh.link_up.domain.SocialUser;
import kh.link_up.domain.Users;
import kh.link_up.repository.BoardRepository;
import kh.link_up.repository.SocialUserRepository;
import kh.link_up.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
public class BoardFileService {

    private final BoardRepository boardRepository;
    private final UsersService usersService;
    private final SocialUserRepository socialUserRepository;
    private final BoardCacheService boardCacheService;
    private final UserUtil userUtil;

    /**
     * 작성자를 할당하고 게시글을 저장하는 로직
     */
    public void assignWriterAndSaveBoard(Board board, Principal principal, Authentication authentication) {
        boolean isSaved = false;

        // UserUtil을 통해 닉네임 또는 이메일 등 사용자 식별자 가져오기
        String userIdentifier = userUtil.getUserIdentifier(principal, authentication);

        // 일반 사용자 여부 체크 (닉네임으로 usersService 조회)
        Users writer = usersService.findByNickname(userIdentifier);
        if (writer != null) {
            board.setWriter(writer);
            boardRepository.save(board);
            isSaved = true;
            log.debug("일반 사용자 게시글 저장됨");
        } else {
            // 소셜 사용자 여부 체크 (이메일로 socialUserRepository 조회)
            SocialUser socialUser = socialUserRepository.findByEmail(userIdentifier);
            if (socialUser != null) {
                board.setSocialUser(socialUser);
                boardRepository.save(board);
                isSaved = true;
                log.debug("소셜 사용자 게시글 저장됨");
            } else {
                log.debug("사용자 정보를 찾을 수 없습니다.");
            }
        }

        if (isSaved) {
            if ("NOTICE".equalsIgnoreCase(board.getCategory())) {
                log.info("공지사항 캐시 재등록 시작");
                boardCacheService.refreshNoticeBoardCache(); // 캐시 초기화 + 재등록
            }
        }
    }

    // 유틸메서드 : 게시글 파일 저장
    public void saveFilePath(Long bIdx, String filePath) {

        Board board = boardRepository.findById(bIdx).get();
        if (board != null) {
            board.setFilePath(filePath);
            boardRepository.save(board);
        } else {
            throw new IllegalArgumentException("게시글 파일 저장 오류발생");
        }
    }

    // 유틸리티 메서드: 디렉토리 생성
    private void createDirectoryIfNotExists(String dirPath) throws IOException {
        if (!Files.exists(Paths.get(dirPath))) {
            Files.createDirectories(Paths.get(dirPath));
        } else {
            log.debug("디렉토리 만드는데 오류남 하...");
        }
    }

    //유틸메서드 : 게시글 폴더 및 이름 저장
    public void saveFiles(Long boardId, String userNickname, String boardTitle, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) return;

        String baseDir = "D:\\LinkUpFileFolder\\(게시판)" + userNickname + "\\" + boardTitle;

        // 실제 파일이 하나라도 존재하는지 확인
        boolean hasValidFile = files.stream().anyMatch(file -> !file.isEmpty());
        if (!hasValidFile) return; // 진짜 파일 없으면 종료

        createDirectoryIfNotExists(baseDir);

        for (MultipartFile file : files) {
            try {
                String uniqueFilename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                String fullPath = baseDir + "\\" + uniqueFilename;

                file.transferTo(new File(fullPath));
                saveFilePath(boardId, uniqueFilename);

            } catch (IOException e) {
                log.error("파일 저장 실패: {}", e.getMessage(), e);
                // 필요하다면 예외 다시 throw해서 컨트롤러에서 처리하게 해도 됨
            }
        }
    }
}

