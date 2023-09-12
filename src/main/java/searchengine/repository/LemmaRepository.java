package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    int countBySiteId(int siteId);

    @Query(value = "select lemma from lemma where frequency > ?1 and lemma = ?2 and site_id = ?3", nativeQuery = true)
    String findLemmaByondEdgeFreq(Integer edgeValue, String lemmaName, Integer siteId);

}
