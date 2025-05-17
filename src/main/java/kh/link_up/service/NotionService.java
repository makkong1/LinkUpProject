package kh.link_up.service;

import kh.link_up.converter.NotionConverter;
import kh.link_up.domain.Notion;
import kh.link_up.domain.Users;
import kh.link_up.dto.NotionDTO;
import kh.link_up.repository.NotionRepository;
import kh.link_up.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class NotionService {

    private final NotionRepository notionRepository;
    private final NotionConverter notionConverter;
    private final UsersRepository usersRepository;

    // 노션전체 가져오기
    public List<NotionDTO> getAllNotion(Long u_idx) {
        log.debug("service u_idx : {}", u_idx);
        List<Notion> notions = notionRepository.findByWriter_u_idx(u_idx);
        log.debug("service notions : {}", notions);

        return notions.stream()
                .map(notionConverter::convertToDTO)
                .collect(Collectors.toList());
    }

    // 특정 노션 가져오기
    public NotionDTO getNotionById(Long n_idx){
        log.debug("n_idx : {}", n_idx);
        Notion notion = notionRepository.findById(n_idx).orElse(null);
        log.debug("특정노션 찾기 : {}", notion);

        return (notion != null) ? notionConverter.convertToDTO(notion) : null;
    }

    // 저장이랑 수정 같은 메서드에 묶음
    public NotionDTO saveNotion(Long uIdx, Long n_idx, NotionDTO notionDTO) {
        Users user = usersRepository.findById(uIdx).orElse(null);
        log.debug("노션저장하는 user 정보: {}", user);

        if(user == null) {
            throw new RuntimeException("유저를 찾을 수 없습니다.");
        }

        if(n_idx == null){
            log.debug("노션정보 없음");
            return createNotion(uIdx, notionDTO, user);
        } else {
            log.debug("노션정보 있음");
            return updateNotion(n_idx, notionDTO);
        }
    }

    //notion 수정
    private NotionDTO updateNotion(Long n_idx, NotionDTO notionDTO) {
        log.debug("updateNotion n_idx : {}", n_idx);
        Notion notion = notionRepository.findById(n_idx)
                .orElseThrow(() -> new RuntimeException("Notion을 찾을 수 없습니다."));

        notion.setNTitle(notionDTO.getN_title());
        notion.setNContent(notionDTO.getN_content());

        // null 방지용 기본값 설정
        notion.setNUploadTime(notionDTO.getN_upload() != null ? notionDTO.getN_upload() : LocalDateTime.now());

        notionRepository.save(notion);
        return notionConverter.convertToDTO(notion);
    }

    //notion 저장
    private NotionDTO createNotion(Long uidx, NotionDTO notionDTO, Users user) {
        log.debug("createNotion uidx : {}", uidx);
        Notion notion = new Notion();
        notion.setNTitle(notionDTO.getN_title());
        notion.setNContent(notionDTO.getN_content());
        notion.setWriter(user);
        notion.setNViewCount(0);  // 기본 값
        notion.setNLikeCount(0);  // 기본 값

        // null 방지용 기본값 설정
        notion.setNUploadTime(notionDTO.getN_upload() != null ? notionDTO.getN_upload() : LocalDateTime.now());

        notionRepository.save(notion);
        return notionConverter.convertToDTO(notion);
    }

    public void deleteNotion(Long n_idx){
        log.debug("deleteNotion n_idx : {}", n_idx);
        notionRepository.deleteById(n_idx);
    }

    // 파일경로 db 저장
    public void saveFilePath(Long nIdx, String filePath) {
        log.debug("filepath: {}", filePath);
        Optional<Notion> findNotion = notionRepository.findById(nIdx);
        if(findNotion.isPresent()){
            Notion notion = findNotion.get();
            notion.setNFilePath(filePath);
            notionRepository.save(notion);  // 변경된 파일 경로 저장
        }
    }
}
