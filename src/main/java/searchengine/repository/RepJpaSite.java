package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

import java.util.List;

@Transactional(isolation = Isolation.SERIALIZABLE)
@Repository
public interface RepJpaSite extends JpaRepository<Site, Integer> {
    List<Site> findByUrl(String url);
}




