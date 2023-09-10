package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    @Query(value = "select * from `index` where lemma_id = " +
            "(select id from Lemma where lemma = :value and site_id = (select id from Site where url = :url))"
            , nativeQuery = true)
    List<IndexEntity> selectIndexWereLemma(String value, String url);

    List<IndexEntity> findByLemmaId(int lemmaId);
}
