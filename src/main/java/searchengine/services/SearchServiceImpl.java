package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.LemmaFinderInstance;
import searchengine.config.SiteMapBean;
import searchengine.config.pojo.RankAndIdPage;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SnippedObject;
import searchengine.dto.search.SearchRequest;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static searchengine.config.pojo.StaticMetods.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SiteMapBean siteMapBean;
    private final LemmaFinderInstance lemmaFinderInstance;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;

    @Override
    public SearchResponse search(SearchRequest req) {
        try {
            Set<String> lemmaSet = getLemmaSet(req.getQuery());
            List<RankAndIdPage> rankAndIdPages = getPageList(lemmaSet, req.getQuery());
            Collections.sort(rankAndIdPages);
            List<RankAndIdPage> limitedList = getLimitedList(rankAndIdPages, req.getOffset(), req.getLimit());
            List<SnippedObject> snippedObjectList = getListSnippetObject(limitedList, req.getQuery());
            return getSearchRespOk(snippedObjectList);
        } catch (Exception e) {
            e.printStackTrace();
            return getSearchresponsError(e.getMessage());
        }
    }

    public Set<String> getLemmaSet(String txt) throws Exception {
        Set<String> lemmaSet = lemmaFinderInstance.getLemmaFinder().getLemmaSet(txt);
        if (lemmaSet.isEmpty()) {
            throw new Exception("Передана пустая строка");
        }
        return lemmaSet;
    }

    public List<RankAndIdPage> getPageList(Set<String> lemmaSet, String siteUrl) throws Exception {
        List<SiteEntity> listSites = siteUrl == null ? siteRepository.findByUrl(siteUrl) :
                siteRepository.findAll();
        if (listSites.isEmpty()) {
            throw new Exception("Идексация не проводилась");
        }
        if (listSites.size() == 1) {
            return new LinkedList<>(creatQueryAndFindPage(lemmaSet, listSites.get(0)));
        }
        return getPageListOfAllSite(lemmaSet, listSites);
    }

    public List<RankAndIdPage> creatQueryAndFindPage(Set<String> lemmasNames, SiteEntity site) {
        int countPage = pageRepository.countBySiteId(site.getId());
        if (countPage < 10) {
            return findPage(new ArrayList<>(lemmasNames), site);
        }
        int edgeValue = countPage - countPage / 4;
        List<String> creatLemmQuery = creatQuery(edgeValue, lemmasNames, site);
        return findPage(creatLemmQuery, site);
    }

    public List<RankAndIdPage> getPageListOfAllSite(Set<String> lemmaSet, List<SiteEntity> listSites) {
        List<RankAndIdPage> pages = new LinkedList<>();
        for (SiteEntity site : listSites) {
            pages.addAll(creatQueryAndFindPage(lemmaSet, site));
        }
        return pages;
    }

    public List<String> creatQuery(int edgeValue, Set<String> lemmasNames, SiteEntity site) {
        List<String> creatLemmQuery = new ArrayList<>(lemmasNames);
        for (String lemma : creatLemmQuery) {
            String lemByonFreq = lemmaRepository.findLemmaByondEdgeFreq(edgeValue, lemma, site.getId());
            if (lemByonFreq != null) {
                creatLemmQuery.remove(lemByonFreq);
            }
        }
        if (creatLemmQuery.isEmpty()) {
            return new ArrayList<>(lemmasNames);
        }
        return creatLemmQuery;
    }

    public List<RankAndIdPage> findPage(List<String> lemmaNames, SiteEntity site) {
        List<LemmaEntity> lemmas = getSortLemmas(lemmaNames, site);
        List<RankAndIdPage> listRankAndId = new ArrayList<>();
        for (int i = 0; i < lemmas.size(); i++) {
            LemmaEntity lemma = lemmas.get(i);
            if (i == 0) {
                List<Integer> pageIdList = indexRepository.pageIdList(lemma.getId());
                listRankAndId = getRankAndIdListPage(lemma.getId(), pageIdList, listRankAndId);
            }
            if (i > 0) {
                listRankAndId = filterPageList(listRankAndId, lemma.getId());
            }
        }
        if (!listRankAndId.isEmpty()) {
            reWriteRank(listRankAndId);
        }
        return listRankAndId;
    }

    public List<RankAndIdPage> getRankAndIdListPage(int lemmaId
            , List<Integer> pagesId, List<RankAndIdPage> rankAndIdList) {
        for (Integer pageId : pagesId) {
            List<Float> listRank = indexRepository.findRankByLemmaIdAndPageId(lemmaId, pageId);
            if (!listRank.isEmpty()) {
                Float rank = listRank.get(0);
                rankAndIdList.add(new RankAndIdPage(pageId, rank));
            }
        }
        return rankAndIdList;
    }

    public List<RankAndIdPage> filterPageList(List<RankAndIdPage> rankAndIdPageList, int lemmaId) {
        for (RankAndIdPage rankAndId : rankAndIdPageList) {
            List<Integer> lemmaList = indexRepository.lemmaIdList(rankAndId.getPageId());
            if (!lemmaList.contains(lemmaId)) {
                rankAndIdPageList.remove(rankAndId);
                continue;
            }
            List<Float> list = indexRepository.findRankByLemmaIdAndPageId(lemmaId, rankAndId.getPageId());
            if (!list.isEmpty()) {
                Float rank = list.get(0);
                Float generalRenk = rankAndId.getGeneralRank();
                rankAndId.setGeneralRank(generalRenk + rank);
            }
        }
        return rankAndIdPageList;
    }

    public void reWriteRank(List<RankAndIdPage> rankAndIdPageList) {
        float allPagesGeneralRank = rankAndIdPageList
                .stream()
                .map(RankAndIdPage::getGeneralRank).reduce(Float::sum).get();
        for (RankAndIdPage rankAndIdPage : rankAndIdPageList) {
            Float valueRankNew = rankAndIdPage.getGeneralRank() / allPagesGeneralRank;
            rankAndIdPage.setGeneralRank(valueRankNew);
        }
    }

    public List<LemmaEntity> getSortLemmas(List<String> lemmaNames, SiteEntity site) {
        Map<String, LemmaEntity> lemmasMap = siteMapBean.getSiteMap().get(site.getUrl()).getLemmaMap();
        List<LemmaEntity> lemmas = getLemmasFromNamesList(lemmasMap, lemmaNames);
        lemmas.sort(LemmaEntity.getFrequencyComparator());
        return lemmas;
    }

    public List<LemmaEntity> getLemmasFromNamesList(Map<String, LemmaEntity> lemmasMap, List<String> lemmaNames) {
        List<LemmaEntity> lemmaList = new ArrayList<>();
        for (String lemmaName : lemmaNames) {
            if (lemmasMap.containsKey(lemmaName)) {
                LemmaEntity lemma = lemmasMap.get(lemmaName);
                lemmaList.add(lemma);
            }
        }
        return lemmaList;
    }

    public List<SnippedObject> getListSnippetObject(List<RankAndIdPage> rankAndIdPageList, String requestQuery) {
        List<SnippedObject> snippeds = new LinkedList<>();
        for (RankAndIdPage rankAndIdPage : rankAndIdPageList) {
            Optional<PageEntity> pageOpt = pageRepository.findById(rankAndIdPage.getPageId());
            if (pageOpt.isPresent()) {
                PageEntity p = pageOpt.get();
                String snipped = getSnipped(p.getContent(), requestQuery);
                snippeds.add(new SnippedObject(
                                cleanSlashUrl(p.getSite().getUrl())
                                , p.getSite().getName()
                                , p.getPath()
                                , getTitle(p.getContent())
                                , snipped
                        )
                );
            }
        }
        return snippeds;
    }

    public String getTitle(String str) {
        Document doc = Jsoup.parse(str);
        Elements elementsHead = doc.select("head >title");
        return elementsHead.text();
    }

    public List<RankAndIdPage> getLimitedList(List<RankAndIdPage> rankAndIdPageList, int offset, int limit) {
        List<RankAndIdPage> limitedRankAndIdPageList = new ArrayList<>();
        if (offset > rankAndIdPageList.size()) {
            offset = 0;
        }
        limit = Math.min(offset + limit, rankAndIdPageList.size());
        for (; offset < limit; offset++) {
            limitedRankAndIdPageList.add(rankAndIdPageList.get(offset));
        }
        return limitedRankAndIdPageList;
    }

    public String getSnipped(String txt, String queryTxt) {
        StringBuilder build = new StringBuilder();
        int start = 0;
        int stop = txt.length();
        int entryPoint = txt.indexOf(queryTxt);
        int exitPoint = entryPoint + queryTxt.length() - 1;
        if (entryPoint > 80) {
            start = entryPoint - 80;
        }
        if (txt.length() - exitPoint > 80) {
            stop = exitPoint + 80;
        }
        for (; start < stop; start++) {
            build.append(txt.charAt(start));
            if (start == entryPoint - 1) {
                build.append("><b>");
            }
            if (start == exitPoint) {
                build.append("</b><");
            }
        }
        return build.toString();
    }

}
