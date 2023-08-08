package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

import java.util.List;

@Repository
public interface RepJpaIndex extends JpaRepository<Index, Integer> {
    @Query(value = "select* from `index` where lemma_id = (select id from Lemma where lemma = :value and site_id = (select id from Site where url = :url))", nativeQuery = true)
    List<Index> selectIndexWereLemma(String value, String url);
}
