package kh.link_up.repository;

import kh.link_up.domain.Board;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true) // ✅ Repository 전체에 read-only 적용 (쓰기 쿼리는 @Modifying에만)
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 개선:
    // 정렬 기준(ORDER BY b_upload DESC 같은 것)을 명확히 주면 페이징 성능 ↑
    // 인덱스 없으면 LIMIT/OFFSET 성능 저하
    Page<Board> findAll(Pageable pageable);

    // =======================
    // 유저용 검색 (FullText / NativeQuery)
    // =======================

    @Query(value = "SELECT * FROM board WHERE category <> :excludeCategory AND MATCH(b_title) AGAINST(:text IN BOOLEAN MODE)",
            countQuery = "SELECT COUNT(*) FROM board WHERE category <> :excludeCategory AND MATCH(b_title) AGAINST(:text IN BOOLEAN MODE)",
            nativeQuery = true)
    Page<Board> searchByTitleForUsers(@Param("text") String text,
                                      @Param("excludeCategory") String excludeCategory,
                                      Pageable pageable);

    @Query(value = "SELECT * FROM board WHERE category <> :excludeCategory AND MATCH(b_content) AGAINST(:text IN BOOLEAN MODE)",
            countQuery = "SELECT COUNT(*) FROM board WHERE category <> :excludeCategory AND MATCH(b_content) AGAINST(:text IN BOOLEAN MODE)",
            nativeQuery = true)
    Page<Board> searchByContentForUsers(@Param("text") String text,
                                        @Param("excludeCategory") String excludeCategory,
                                        Pageable pageable);

    @Query(value = "SELECT * FROM board WHERE category <> :excludeCategory AND b_writer LIKE CONCAT(:text, '%')",
            countQuery = "SELECT COUNT(*) FROM board WHERE category <> :excludeCategory AND b_writer LIKE CONCAT(:text, '%')",
            nativeQuery = true)
    Page<Board> searchByWriterForUsers(@Param("text") String text,
                                       @Param("excludeCategory") String excludeCategory,
                                       Pageable pageable);

    // =======================
    // 관리자용 검색 (FullText / NativeQuery)
    // =======================

    // 제목으로 검색 + 페이징 처리
    // 개선:
    // 검색어에서만 공백 제거 후 → 컬럼은 그대로 인덱스 활용
    // MySQL FULLTEXT 인덱스 고려 (제목/내용 검색 시 최적)
//    @Query("SELECT b FROM Board b WHERE REPLACE(b.title, ' ', '') LIKE REPLACE(CONCAT('%', :title, '%'), ' ', '')")
//    Page<Board> searchByTitle(@Param("title") String title, Pageable pageable);
    // 밑에 꺼가 인덱스 적용 한거
    @Query(value = "SELECT * FROM Board b WHERE MATCH(b_title) AGAINST(:title IN BOOLEAN  MODE)",
            countQuery = "SELECT COUNT(*) FROM Board b WHERE MATCH(b_title) AGAINST(:title IN BOOLEAN  MODE)",
            nativeQuery = true)
    Page<Board> searchByTitle(@Param("title") String title, Pageable pageable);

    // 작성자로 검색 + 페이징 처리
    // 개선:
    // 앞부분 일치 검색만 (LIKE 'abc%')이면 인덱스 사용 가능
    // 아니면 FULLTEXT or ElasticSearch 같은 검색엔진 고려
//    @Query("SELECT b FROM Board b WHERE b.writer.uUsername LIKE :writer%")
//    Page<Board> searchByWriter(@Param("writer") String writer, Pageable pageable);
    @Query(value = "SELECT * FROM board b WHERE b_writer LIKE CONCAT(:writer, '%')",
            countQuery = "SELECT COUNT(*) FROM board b WHERE b_writer LIKE CONCAT(:writer, '%')",
            nativeQuery = true)
    Page<Board> searchByWriter(@Param("writer") String writer, Pageable pageable);

    // 내용으로 검색 + 페이징 처리
    // 개선
    // MySQL FULLTEXT(content) 인덱스 활용 (MATCH(content) AGAINST(...))
    // 그게 어렵다면 최소한 LIKE 'keyword%' 패턴으로 제한
//    @Query("SELECT b FROM Board b WHERE b.content LIKE: content%")
//    Page<Board> searchByContent(@Param("content") String content, Pageable pageable);
    @Query(value = "SELECT * FROM board WHERE MATCH(b_content) AGAINST(:content IN BOOLEAN MODE)",
            countQuery = "SELECT COUNT(*) FROM board WHERE MATCH(b_content) AGAINST(:content IN BOOLEAN MODE)",
            nativeQuery = true)
    Page<Board> searchByContent(@Param("content") String content, Pageable pageable);

    Optional<Board> findById(Long id);

    //실행계획:
    //PK 조건 (b_idx) → 인덱스 사용 → O(1) 성능
    // 문제없을듯?
    @Modifying
    @Transactional
    @Query("update Board b set b.viewCount = b.viewCount + 1 where b.id = :id")
    int incrementViewCount(@Param("id") Long id);

    // b_category 에 인덱스 없으면 풀스캔
    @EntityGraph(attributePaths = {"writer", "socialUser"})
    Page<Board> findByCategory(String category, Pageable pageable);

//    // 개선:
//    // 쿼리 동적으로 분리 (조건별로 JPQL 따로 작성 → OR 줄이기)
//    // title, nickname, content 각각에 FULLTEXT 인덱스 도입 고려
//    @EntityGraph(attributePaths = {"writer", "socialUser"})
//    @Query("""
//                SELECT b FROM Board b
//                WHERE b.category <> :excludeCategory
//                AND (
//                  (:selectValue = 'title' AND b.title LIKE %:text%) OR
//                  (:selectValue = 'writer' AND b.writer.uNickname LIKE %:text%) OR
//                  (:selectValue = 'content' AND b.content LIKE %:text%)
//                )
//            """)
//    Page<Board> searchByCriteria(@Param("selectValue") String selectValue,
//                                 @Param("text") String text,
//                                 @Param("excludeCategory") String excludeCategory,
//                                 Pageable pageable);

    //실행계획:
    //PK 조건 (b_idx) → 인덱스 사용 O(1)
    @Modifying
    @Transactional
    @Query("UPDATE Board b SET b.likeCount = b.likeCount + :likeCount, b.dislikeCount = b.dislikeCount + :dislikeCount WHERE b.bIdx = :boardId")
    int updateLikeDislikeCount(Long boardId, int likeCount, int dislikeCount);

    List<Board> findByCategory(String category);

}
