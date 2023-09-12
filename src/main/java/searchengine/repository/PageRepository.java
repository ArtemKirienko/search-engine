package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    int countBySiteId(int siteId);

    List<PageEntity> findByPathAndSiteId(String path, int siteId);

    Optional<PageEntity> findById(int pageId);
}
