package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
//////////////////переделать на обычные запросы
    @Query(value = "select page_id from `index` where lemma_id = :lemmaId", nativeQuery = true)
    List<Integer> pageIdList(int lemmaId);

    @Query(value = "select lemma_id from `index` where page_id = :pageId", nativeQuery = true)
    List<Integer> lemmaIdList(int pageId);

    @Query(value = "select `rank` from `index` where lemma_id = :lemmaId and page_id = :pageId", nativeQuery = true)
    List<Float> findRankByLemmaIdAndPageId(int lemmaId, int pageId);
}
