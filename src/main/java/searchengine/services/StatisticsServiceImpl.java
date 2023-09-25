package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.ExecuteIndicator;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;
import searchengine.repository.PageRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final ExecuteIndicator executeIndicator;
    private final SiteRepository repositoryJpaSite;
    private final PageRepository repositoryJpaPage;
    private final LemmaRepository repositoryJpaLemma;
    private List<SiteEntity> allSites;

    @Override
    public StatisticsResponse getStatistics() {
        allSites = repositoryJpaSite.findAll();
        int sitesCount = allSites.size();
        boolean indexing = executeIndicator.isExec();
        TotalStatistics total = new TotalStatistics(sitesCount, indexing);
        List<DetailedStatisticsItem> detailed = createDataDetailedAndTotal(total);
        StatisticsData data = new StatisticsData(total, detailed);
        return new StatisticsResponse(true, data);
    }

    public List<DetailedStatisticsItem> createDataDetailedAndTotal(TotalStatistics total) {
        List<DetailedStatisticsItem> detailedList = new ArrayList<>();
        for (SiteEntity site : allSites) {
            int pages = repositoryJpaPage.countBySiteId(site.getId());
            int lemmas = repositoryJpaLemma.countBySiteId(site.getId());
            DetailedStatisticsItem item = new DetailedStatisticsItem(site.getUrl(), site.getName(),
                    site.getStatus().toString(), site.getStatusTime(), site.getLastError(), pages, lemmas);
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailedList.add(item);
        }
        return detailedList;
    }
}
