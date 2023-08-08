package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.repository.RepJpaLemma;
import searchengine.repository.RepJpaSite;
import searchengine.repository.RepJpaPage;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    RepJpaSite repositoryJpaSite;
    @Autowired
    RepJpaPage repositryJpaPage;
    @Autowired
    RepJpaLemma repositoryJpaLemma;
    private List<Site> allSites;

    @Override
    public StatisticsResponse getStatistics() {
        allSites = repositoryJpaSite.findAll();
        TotalStatistics total = new TotalStatistics();
        total.setSites(allSites.size());
        total.setIndexing(true);
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        createDataDetailedAndTotal(total, detailed);
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    public void createDataDetailedAndTotal(TotalStatistics total, List detailed) {
        allSites.stream().forEach(s -> {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(s.getName());
            item.setUrl(s.getUrl());
            int pages = repositryJpaPage.countBySiteId(s.getId());
            int lemmas = repositoryJpaLemma.countBySiteId(s.getId());
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(s.getStatus().toString());
            item.setError(s.getLastError());
            item.setStatusTime(s.getStatusTime());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        });
    }
}
