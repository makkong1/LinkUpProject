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

import io.micrometer.common.lang.NonNull;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface BoardRepository extends JpaRepository<Board, Long> {

  @NonNull
  Page<Board> findAll(@NonNull Pageable pageable);

  // =======================
  // 유저용 검색 (NativeQuery) / 인덱스 활용 / fulltext 검색
  // =======================
  @Query(value = "SELECT * FROM board WHERE b_category <> :excludeCategory AND MATCH(b_title) AGAINST(:text IN BOOLEAN MODE) ORDER BY b_upload DESC", countQuery = "SELECT COUNT(*) FROM board WHERE b_category <> :excludeCategory AND MATCH(b_title) AGAINST(:text IN BOOLEAN MODE)", nativeQuery = true)
  Page<Board> searchByTitleForUsers(@Param("text") String text,
      @Param("excludeCategory") String excludeCategory,
      Pageable pageable);

  @Query(value = "SELECT * FROM board WHERE b_category <> :excludeCategory AND MATCH(b_content) AGAINST(:text IN BOOLEAN MODE) ORDER BY b_upload DESC", countQuery = "SELECT COUNT(*) FROM board WHERE b_category <> :excludeCategory AND MATCH(b_content) AGAINST(:text IN BOOLEAN MODE)", nativeQuery = true)
  Page<Board> searchByContentForUsers(@Param("text") String text,
      @Param("excludeCategory") String excludeCategory,
      Pageable pageable);

  @Query(value = "SELECT * FROM board WHERE b_category <> :excludeCategory AND b_writer LIKE CONCAT(:text, '%') ORDER BY b_upload DESC", countQuery = "SELECT COUNT(*) FROM board WHERE b_category <> :excludeCategory AND b_writer LIKE CONCAT(:text, '%')", nativeQuery = true)
  Page<Board> searchByWriterForUsers(@Param("text") String text,
      @Param("excludeCategory") String excludeCategory,
      Pageable pageable);

  // 검색: 제목 + 내용 통합 검색 (Fulltext) / 복합인덱스 활용 / 유저용
  @Query(value = """
      SELECT * FROM board
      WHERE b_category <> :excludeCategory
        AND MATCH(b_title, b_content) AGAINST(:text IN BOOLEAN MODE)
      ORDER BY b_upload DESC
      """, countQuery = """
      SELECT COUNT(*) FROM board
      WHERE b_category <> :excludeCategory
        AND MATCH(b_title, b_content) AGAINST(:text IN BOOLEAN MODE)
      """, nativeQuery = true)
  Page<Board> searchByTitleAndContentForUsers(
      @Param("text") String text,
      @Param("excludeCategory") String excludeCategory,
      Pageable pageable);

  // =======================
  // 관리자용 검색 (NativeQuery) / 인덱스 활용 / fulltext 검색
  // =======================
  @Query(value = "SELECT * FROM board WHERE MATCH(b_title) AGAINST(:title IN BOOLEAN MODE) ORDER BY b_upload DESC", countQuery = "SELECT COUNT(*) FROM board WHERE MATCH(b_title) AGAINST(:title IN BOOLEAN MODE)", nativeQuery = true)
  Page<Board> searchByTitle(@Param("title") String title, Pageable pageable);

  @Query(value = "SELECT * FROM board WHERE b_writer LIKE CONCAT(:writer, '%') ORDER BY b_upload DESC", countQuery = "SELECT COUNT(*) FROM board WHERE b_writer LIKE CONCAT(:writer, '%')", nativeQuery = true)
  Page<Board> searchByWriter(@Param("writer") String writer, Pageable pageable);

  @Query(value = "SELECT * FROM board WHERE MATCH(b_content) AGAINST(:content IN BOOLEAN MODE) ORDER BY b_upload DESC", countQuery = "SELECT COUNT(*) FROM board WHERE MATCH(b_content) AGAINST(:content IN BOOLEAN MODE)", nativeQuery = true)
  Page<Board> searchByContent(@Param("content") String content, Pageable pageable);

  // 검색: 제목 + 내용 통합 검색 (Fulltext) / 복합인덱스 활용 / 관리자용
  @Query(value = """
      SELECT * FROM board
      WHERE MATCH(b_title, b_content) AGAINST(:text IN BOOLEAN MODE)
      ORDER BY b_upload DESC
      """, countQuery = """
      SELECT COUNT(*) FROM board
      WHERE MATCH(b_title, b_content) AGAINST(:text IN BOOLEAN MODE)
      """, nativeQuery = true)
  Page<Board> searchByTitleAndContentForAdmin(@Param("text") String text, Pageable pageable);

  @Modifying
  @Transactional
  @Query(value = "UPDATE board SET b_view_cnt = b_view_cnt + 1 WHERE b_idx = :id", nativeQuery = true)
  int incrementViewCount(@Param("id") Long id);

  @Modifying
  @Transactional
  @Query(value = "UPDATE board SET b_like = b_like + :likeCount, b_dislike = b_dislike + :dislikeCount WHERE b_idx = :boardId", nativeQuery = true)
  int updateLikeDislikeCount(@Param("boardId") Long boardId,
      @Param("likeCount") int likeCount,
      @Param("dislikeCount") int dislikeCount);

  // 카테고리 조회 (EntityGraph로 writer, socialUser 미리 fetch)
  @EntityGraph(attributePaths = { "writer", "socialUser" })
  Page<Board> findByCategoryOrderByUploadTimeDesc(String category, Pageable pageable);

  List<Board> findByCategoryOrderByUploadTimeDesc(String category);
}
