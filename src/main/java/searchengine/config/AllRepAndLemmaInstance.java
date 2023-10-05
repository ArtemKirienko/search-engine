package searchengine.config;

import lombok.Getter;
import org.springframework.stereotype.Component;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

@Getter
@Component
public class AllRepAndLemmaInstance {
    private final SiteRepository repJpaSite;
    private final IndexRepository repJpaIndex;
    private final LemmaRepository repJpaLemma;
    private final PageRepository repJpaPage;
    private final LemmaFinderInstance lemmaFinderInstance;
    private final ConnectionMetod connectionMetod;
    private final SitesList sitesList;


    public AllRepAndLemmaInstance(SiteRepository repJpaSite, IndexRepository repJpaIndex, LemmaRepository repJpaLemma, PageRepository repJpaPage, LemmaFinderInstance lemmaFinderInstance, ConnectionMetod connectionMetod, SitesList sitesList) {
        this.repJpaSite = repJpaSite;
        this.repJpaIndex = repJpaIndex;
        this.repJpaLemma = repJpaLemma;
        this.repJpaPage = repJpaPage;
        this.lemmaFinderInstance = lemmaFinderInstance;
        this.connectionMetod = connectionMetod;
        this.sitesList = sitesList;
    }
}
