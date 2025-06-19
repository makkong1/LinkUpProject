package kh.link_up.repository;

import kh.link_up.domain.Comment;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("select c from Comment c where c.writer.uNickname like %:writer%")
    Page<Comment> findByWriterContaining(String writer, Pageable pageable);

    @Query("select c from Comment c where c.cContent like %:content%")
    Page<Comment> findByContentContaining(String content, Pageable pageable);

    Page<Comment> findAll(Pageable pageable);

    @Query("select c from Comment c where c.board.bIdx = :bIdx")
    Page<Comment> findCommentByBIdx(@Param("bIdx") Long bIdx, Pageable pageable);

    @Query("select c from Comment c where c.cReport >= 1")
    Page<Comment> findReportComment(Pageable pageable);

    // CommentRepository
    @Modifying
    @Query("UPDATE Comment c SET c.cLike = c.cLike + :like, c.cDislike = c.cDislike + :dislike WHERE c.id = :id")
    int updateLikeDislikeCount(@Param("id") Long id, @Param("like") int like, @Param("dislike") int dislike);

}
