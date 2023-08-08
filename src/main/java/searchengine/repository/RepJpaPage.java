package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

@Repository
public interface RepJpaPage extends JpaRepository<Page, Integer> {
    int countBySiteId(int siteId);
}
