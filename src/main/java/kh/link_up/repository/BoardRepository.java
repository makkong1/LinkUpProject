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
public interface BoardRepository extends JpaRepository<Board, Long> {

    Page<Board> findAll(Pageable pageable);

    // 제목으로 검색 + 페이징 처리
    @Query("SELECT b FROM Board b WHERE REPLACE(b.title, ' ', '') LIKE REPLACE(CONCAT('%', :title, '%'), ' ', '')")
    Page<Board> searchByTitle(@Param("title") String title, Pageable pageable);

    // 작성자로 검색 + 페이징 처리
    @Query("SELECT b FROM Board b WHERE b.writer.uUsername LIKE %:writer%")
    Page<Board> searchByWriter(@Param("writer") String writer, Pageable pageable);

    // 내용으로 검색 + 페이징 처리
    @Query("SELECT b FROM Board b WHERE b.content LIKE %:content%")
    Page<Board> searchByContent(@Param("content") String content, Pageable pageable);

    Optional<Board> findById(Long id);

    @EntityGraph(attributePaths = {"writer", "socialUser"})
    Page<Board> findByCategory(String category, Pageable pageable);

    @EntityGraph(attributePaths = {"writer", "socialUser"})
    @Query("""
                SELECT b FROM Board b 
                WHERE b.category <> :excludeCategory 
                AND (
                  (:selectValue = 'title' AND b.title LIKE %:text%) OR 
                  (:selectValue = 'writer' AND b.writer.uNickname LIKE %:text%) OR 
                  (:selectValue = 'content' AND b.content LIKE %:text%)
                )
            """)
    Page<Board> searchByCriteria(@Param("selectValue") String selectValue,
                                 @Param("text") String text,
                                 @Param("excludeCategory") String excludeCategory,
                                 Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Board b SET b.likeCount = b.likeCount + :likeCount, b.dislikeCount = b.dislikeCount + :dislikeCount WHERE b.bIdx = :boardId")
    int updateLikeDislikeCount(Long boardId, int likeCount, int dislikeCount);

    List<Board> findByCategory(String category);

}
