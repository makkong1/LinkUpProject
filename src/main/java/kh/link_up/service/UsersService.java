package kh.link_up.service;

import jakarta.persistence.EntityNotFoundException;
import kh.link_up.converter.UserConverter;
import kh.link_up.domain.Board;
import kh.link_up.domain.Comment;
import kh.link_up.domain.Notion;
import kh.link_up.domain.Users;
import kh.link_up.dto.UsersDTO;
import kh.link_up.repository.UsersRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = { @Autowired })
public class UsersService {

    private final UsersRepository usersRepository;
    private final UserConverter usersConverter;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JavaMailSenderImpl mailSender;

    // 메모리에 인증번호를 저장할 Map (u_id를 키로, 인증번호와 만료 시간 저장)
    private final Map<String, AuthCode> authCodeStore = new HashMap<>();

    public Users getUserByNickname(String name) {
        return usersRepository.findByuNickname(name);
    }

    // 이메일 인증번호 저장을 위한 내부 클래스
    @Getter
    private static class AuthCode {
        private final String code; // 인증번호
        private final LocalDateTime expirationTime; // 만료 시간

        public AuthCode(String code, LocalDateTime expirationTime) {
            this.code = code;
            this.expirationTime = expirationTime;
        }

        public String getCode() {
            return code;
        }

        // public LocalDateTime getExpirationTime() {
        // return expirationTime;
        // }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expirationTime);
        }
    }

    // 잠금 해제 메서드
    public void unlockUser(String userId) {
        Optional<Users> userOpt = usersRepository.findById(userId);
        if (userOpt.isPresent()) {
            Users user = userOpt.get();
            if (user.isAccountLocked()) {
                user.setAccountLocked(false); // 잠금 해제
                usersRepository.save(user);
            } else {
                throw new IllegalStateException("사용자는 이미 잠금 해제된 상태입니다.");
            }
        } else {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
    }

    public Users findByNickname(String nickname) {
        return usersRepository.findByuNickname(nickname);
    }

    // 사용자 조회
    public List<UsersDTO> getAllUsersFromAdmin() {
        // 모든 사용자 조회
        List<Users> users = usersRepository.findAll();

        // 사용자 리스트가 비지 않았다면 변환 후 반환
        if (!users.isEmpty()) {
            List<UsersDTO> usersDTO = usersConverter.convertList(users);
            log.debug("제발좀;;  : {}", usersDTO);
            return usersDTO;
        }

        // 리스트가 비었을 경우 빈 리스트 반환
        return new ArrayList<>();
    }

    // 특정 사용자 조회
    public Optional<UsersDTO> getUserById(Long u_idx) {
        log.debug("특정사용자 idx : {}", u_idx);
        return usersRepository.findById(u_idx).map(usersConverter::convertToDTO);
    }

    // 회원가입
    public void createUser(UsersDTO usersDTO) {
        log.debug("회원가입 서비스에 들어온 usersDTO : {}", usersDTO);
        // 기본값으로 'USER' 설정
        if (usersDTO.getURole() == null || usersDTO.getURole().isEmpty()) {
            usersDTO.setURole("USER");
        }

        Users user = usersConverter.convertToEntity(usersDTO);
        log.debug("엔티티로 바꾼 user: {}", user);
        user.setPassword(passwordEncoder.encode(usersDTO.getPwd())); // 비밀번호 암호화
        Users savedUser = usersRepository.save(user);

        usersConverter.convertToDTO(savedUser);
    }

    // 사용자 수정
    public void updateUser(Long u_idx, UsersDTO usersDTO) {
        // 사용자 존재 여부 확인
        Optional<Users> existingUser = usersRepository.findById(u_idx);
        if (existingUser.isPresent()) {
            Users user = existingUser.get();
            user.setUNickname(usersDTO.getUNickname());
            user.setUEmail(usersDTO.getUEmail());
            user.setUTelephone(usersDTO.getUTelephone());
            user.setUBirthday(usersDTO.getUBirthday());

            // 비밀번호가 포함되어 있으면 암호화해서 저장
            if (usersDTO.getPwd() != null && !usersDTO.getPwd().isEmpty()) {
                user.setPassword(passwordEncoder.encode(usersDTO.getPwd()));
            }

            usersRepository.save(user); // 업데이트된 사용자 정보 저장
        }
    }

    // 사용자 삭제
    public void deleteUser(Long u_idx) {
        Optional<Users> user = usersRepository.findById(u_idx);
        user.ifPresent(usersRepository::delete);
    }

    // 닉네임 중복 확인
    public boolean isNicknameDuplicate(String nickname) {
        return usersRepository.existsByuNickname(nickname);
    }

    // 아이디 중복 확인
    public boolean isUserIdDuplicate(String id) {
        return usersRepository.existsById(id);
    }

    // 아이디로 이메일 찾기
    public Optional<String> findEmailById(String u_id) {
        Optional<Users> user = usersRepository.findById(u_id);
        return user.map(Users::getUEmail);
    }

    @Transactional
    // 사용자가 작성한 게시물, 댓글, 노션 조회
    public UsersDTO getUserContents(Long idx) {
        // 유저 정보 가져오기 (유저가 없으면 예외 발생)
        Users user = usersRepository.findById(idx)
                .orElseThrow(() -> new EntityNotFoundException(idx + "라는 idx를 가진 유저가 없습니다."));

        // Lazy 로딩을 통해 관련 엔티티들 접근
        List<Board> boards = user.getBoard(); // 보드 리스트
        List<Comment> comments = user.getComments(); // 댓글 리스트
        List<Notion> notions = user.getNotions(); // 노션 리스트

        // 필요한 데이터를 DTO로 묶어서 반환
        return UsersDTO.builder()
                .uNickname(user.getUNickname())
                .boards(boards)
                .comments(comments)
                .notions(notions)
                .build();
    }

    // 인증번호 생성 및 이메일 전송
    @Async
    public CompletableFuture<String> sendAuthCodeToEmail(String u_id, String email) {
        // 인증번호 생성
        String authCode = generateAuthCode(u_id); // u_id를 전달하여 인증번호 생성

        // 이메일 전송
        sendEmail(email, authCode);

        return CompletableFuture.completedFuture(authCode);
    }

    // 인증번호 생성 및 저장
    public String generateAuthCode(String u_id) {
        // 6자리 랜덤 인증번호 생성
        String authCode = String.format("%06d", (int) (Math.random() * 1000000));

        // 인증번호 만료 시간 (5분 후)
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(5);

        // 인증번호를 Map에 저장 (u_id를 키로 사용)
        authCodeStore.put(u_id, new AuthCode(authCode, expirationTime));

        return authCode; // 생성된 인증번호 반환
    }

    // 이메일로 인증번호 전송
    @Async
    private void sendEmail(String toEmail, String authCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("비밀번호 찾기 인증번호입니다.");
        message.setText("인증번호는 " + authCode + " 입니다.");
        mailSender.send(message); // 이메일 전송
    }

    // 인증번호 검증
    @Async
    public boolean verifyAuthCode(String u_id, String authCode) {
        AuthCode storedAuthCode = authCodeStore.get(u_id); // u_id로 저장된 인증번호 가져오기

        // 입력된 인증번호에서 공백 제거
        String trimmedAuthCode = authCode.trim();

        // 인증번호가 없거나 만료되었을 경우
        if (storedAuthCode == null || storedAuthCode.isExpired()) {
            authCodeStore.remove(u_id); // 만료된 인증번호 삭제
            return false;
        }

        // 인증번호가 일치하면 true 반환
        return storedAuthCode.getCode().equals(trimmedAuthCode);
    }

    // 비밀번호 변경
    @Async
    public boolean changePassword(String u_id, String password) {
        Optional<Users> userOptional = usersRepository.findById(u_id);
        log.info("userOptional : {} ", userOptional);
        if (userOptional.isPresent()) {
            Users user = userOptional.get();
            user.setPassword(passwordEncoder.encode(password)); // 비밀번호 암호화 후 업데이트
            user.setFailedLoginAttempts(0);
            user.setAccountLocked(false);
            usersRepository.save(user);
            // 인증번호 사용 후 삭제
            authCodeStore.remove(u_id); // 인증번호 삭제

            return true;
        }
        return false;
    }

    // 아이다로 user 찾기
    public Optional<Users> findByUId(String id) {
        return usersRepository.findById(id);
    }

    // 블락사유 저장 및 블락상태 저장
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUB_AMDIN')")
    public void blockUser(Users foundUser) {
        usersRepository.save(foundUser);
    }

    // 사용자 승격 (관리자 권한 부여)
    @PreAuthorize("hasRole('ADMIN')")
    public void promoteToAdmin(String id) {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setURole("SUB_ADMIN"); // 사용자 역할을 admin으로 변경
        usersRepository.save(user); // 변경된 사용자 정보 저장
    }

    // 사용자 해제 (관리자 권한 제거)
    @PreAuthorize("hasRole('ADMIN')")
    public void demoteFromAdmin(String id) {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setURole("USER"); // 사용자 역할을 user로 변경
        usersRepository.save(user); // 변경된 사용자 정보 저장
    }
}
