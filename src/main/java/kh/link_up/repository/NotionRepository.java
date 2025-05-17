package kh.link_up.repository;

import kh.link_up.domain.Notion;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotionRepository extends JpaRepository<Notion, Long> {

    @Query("SELECT n FROM Notion n WHERE n.writer.uIdx = :idx")
    List<Notion> findByWriter_u_idx(@Param("idx") Long idx);

    Optional<Notion> findById(Long n_idx);
}

