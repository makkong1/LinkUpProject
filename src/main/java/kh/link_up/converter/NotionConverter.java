package kh.link_up.converter;

import kh.link_up.domain.Notion;
import kh.link_up.dto.NotionDTO;
import org.springframework.stereotype.Component;


@Component
public class NotionConverter implements EntitiyConverter<Notion, NotionDTO> {

    // Notion 엔티티를 NotionDTO로 변환
    @Override
    public NotionDTO convertToDTO(Notion entity) {
        if (entity == null) {
            return null;
        }

        // 작성자 닉네임을 String으로 저장
        String writerNickname = entity.getWriter() != null ? entity.getWriter().getUNickname() : null;

        return new NotionDTO(
                entity.getNidX(),               // n_idx
                writerNickname,                  // 작성자 닉네임
                entity.getNTitle(),             // 노션 제목
                entity.getNContent(),           // 노션 내용
                entity.getNViewCount(),          // 조회수
                entity.getNLikeCount(),              // 좋아요 수
                entity.getNUploadTime(),             // 업로드 시간 추가
                entity.getNFilePath()
        );
    }

    // NotionDTO를 Notion 엔티티로 변환
    @Override
    public Notion convertToEntity(NotionDTO dto) {
        if (dto == null) {
            return null;
        }

        // Users 객체는 DTO에 포함되지 않으므로, 외부에서 해당 부분은 처리 필요
        return Notion.builder()
                .nidX(dto.getN_idx())           // n_idx
                .writer(null)                    // 실제 구현에서는 Users 객체를 설정해야 함
                .nTitle(dto.getN_title())       // 노션 제목
                .nContent(dto.getN_content())   // 노션 내용
                .nViewCount(dto.getN_view_cnt()) // 조회수
                .nLikeCount(dto.getN_like())         // 좋아요 수
                .nUploadTime(dto.getN_upload())  // 업로드 시간 (DTO에서 설정된 시간 사용)
                .nFilePath(dto.getN_filepath())
        .build();
    }
}
