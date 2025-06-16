package kh.link_up.converter;

import kh.link_up.domain.Board;
import kh.link_up.dto.BoardDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BoardConverter implements EntitiyConverter<Board, BoardDTO> {

    // n+1 발생
    
    public List<BoardDTO> convertList(List<Board> boardList) {
        return boardList.stream()
                .map(this::convertToDTO) // 개별 Board 객체를 BoardDTO로 변환
                .collect(Collectors.toList());
    }

    @Override
    public BoardDTO convertToDTO(Board board) {
        // 작성자의 닉네임을 일반 사용자 또는 소셜 사용자 중 하나로 설정
        String writerNickname = null;
        if (board.getWriter() != null) {
            writerNickname = board.getWriter().getUNickname(); // 일반 로그인 사용자의 닉네임
        } else if (board.getSocialUser() != null) {
            writerNickname = board.getSocialUser().getName(); // 소셜 로그인 사용자의 닉네임
        }

        return BoardDTO.builder()
                .bIdx(board.getBIdx()) // bIdx
                .bWriter(writerNickname) // 닉네임 설정
                .category(board.getCategory()) // category 열거형 값을 String으로 변환
                .title(board.getTitle()) // title
                .content(board.getContent()) // content
                .password(board.getPassword()) // password
                .uploadTime(board.getUploadTime()) // uploadTime
                .viewCount(board.getViewCount()) // viewCount
                .likeCount(board.getLikeCount()) // likeCount
                .filePath(board.getFilePath()) // filePath
                .isDeleted(board.getIsDeleted()) // isDeleted
                .comments(board.getComments())
                .socialUser(board.getSocialUser())
                .dislikeCount(board.getDislikeCount())
                .build();
    }

    @Override
    public Board convertToEntity(BoardDTO boardDTO) {
        return Board.builder()
                .bIdx(boardDTO.getBIdx()) // bIdx
                .category(boardDTO.getCategory()) // category
                .title(boardDTO.getTitle()) // title
                .content(boardDTO.getContent()) // content
                .password(boardDTO.getPassword()) // password
                .uploadTime(boardDTO.getUploadTime()) // uploadTime
                .viewCount(boardDTO.getViewCount()) // viewCount
                .likeCount(boardDTO.getLikeCount()) // likeCount
                .filePath(boardDTO.getFilePath()) // filePath
                .isDeleted(boardDTO.getIsDeleted()) // isDeleted
                .comments(boardDTO.getComments()) // 댓글들
                .socialUser(boardDTO.getSocialUser())
                .dislikeCount(boardDTO.getDislikeCount())
                .build();
    }
}
