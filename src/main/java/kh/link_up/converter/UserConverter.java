package kh.link_up.converter;

import kh.link_up.domain.Board;
import kh.link_up.domain.Comment;
import kh.link_up.domain.Notion;
import kh.link_up.domain.Users;
import kh.link_up.dto.UsersDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserConverter implements EntitiyConverter<Users, UsersDTO> {

    public List<UsersDTO> convertList(List<Users> usersList) {
        return usersList.stream()
                .map(this::convertToDTO)  // 개별 Users 객체를 UsersDTO로 변환
                .collect(Collectors.toList());
    }

    // 특정 유저가 작성한 게시글, 댓글, 노션을 DTO에 매핑
    public List<UsersDTO> convertContent(Users user,
                                         List<Board> boardList,
                                         List<Comment> commentsList,
                                         List<Notion> notionList) {
        // 유저가 작성한 게시글 필터링
        List<Board> userBoards = boardList.stream()
                .filter(board -> board.getWriter().getUIdx().equals(user.getUIdx()))
                .collect(Collectors.toList());

        // 유저가 작성한 댓글 필터링
        List<Comment> userComments = commentsList.stream()
                .filter(comment -> comment.getWriter().getUIdx().equals(user.getUIdx()))
                .collect(Collectors.toList());

        // 유저가 작성한 노션 필터링
        List<Notion> userNotions = notionList.stream()
                .filter(notion -> notion.getWriter().getUIdx().equals(user.getUIdx()))
                .collect(Collectors.toList());

        // 유저 정보를 UsersDTO로 변환
        UsersDTO userDTO = convertToDTO(user);

        // UsersDTO에 게시글, 댓글, 노션 추가
        userDTO.setBoards(userBoards);   // 유저의 게시글 리스트 설정
        userDTO.setComments(userComments);  // 유저의 댓글 리스트 설정
        userDTO.setNotions(userNotions);    // 유저의 노션 리스트 설정

        // 리스트로 반환 (사용자가 여러 명일 경우 대비)
        return List.of(userDTO);
    }

    @Override
    public UsersDTO convertToDTO(Users user) {
        return UsersDTO.builder()
                .uIdx(user.getUIdx())
                .uUsername(user.getUUsername())
                .id(user.getId())
                .uRole(user.getURole())
                .uNickname(user.getUNickname())
                .pwd(user.getPassword())
                .uEmail(user.getUEmail())
                .uCreatedAt(user.getUCreatedAt())
                .uTelephone(user.getUTelephone())
                .uBirthday(user.getUBirthday())
                .uBlock(user.getUBlockReason())
                .loginFailCount(user.getFailedLoginAttempts())
                .isAccountLocked(user.isAccountLocked())
                .build();
    }

    @Override
    public Users convertToEntity(UsersDTO usersDTO) {
        return Users.builder()
                .uIdx(usersDTO.getUIdx())
                .uUsername(usersDTO.getUUsername())
                .uNickname(usersDTO.getUNickname())
                .id(usersDTO.getId())
                .password(usersDTO.getPwd())
                .uRole(usersDTO.getURole())
                .uNickname(usersDTO.getUNickname())
                .uCreatedAt(usersDTO.getUCreatedAt())
                .uEmail(usersDTO.getUEmail())
                .uTelephone(usersDTO.getUTelephone())
                .uBirthday(usersDTO.getUBirthday())
                .uBlockReason(usersDTO.getUBlock())
                .failedLoginAttempts(usersDTO.getLoginFailCount())
                .accountLocked(usersDTO.isAccountLocked())
                .build();
    }
}
