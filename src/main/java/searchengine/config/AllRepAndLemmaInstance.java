package searchengine.config;

import lombok.Data;
import org.springframework.stereotype.Component;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

@Component
@Data
public class AllRepAndLemmaInstance {
    private final SiteRepository repJpaSite;
    private final IndexRepository repJpaIndex;
    private final LemmaRepository repJpaLemma;
    private final PageRepository repJpaPage;
    private final LemmaFinderInstance lemmaFinderInstance;
}
