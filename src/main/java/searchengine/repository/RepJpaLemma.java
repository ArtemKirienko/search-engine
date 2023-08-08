package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;
import java.util.Set;

@Repository
public interface RepJpaLemma extends JpaRepository<Lemma, Integer> {
    int countBySiteId(int siteId);

    @Query(value = "select frequency from lemma order by frequency desc", nativeQuery = true)
    List<Integer> findByFrequencyOrderByFrequencyDesc();

    @Query(value = "select* from lemma where frequency >= :freq - :param order by frequency asc ", nativeQuery = true)
    Set<Lemma> getLemmaSort(int freq, int param);
}
