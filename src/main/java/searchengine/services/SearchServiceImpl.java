package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.LemmaFinderInstance;
import searchengine.config.SiteMapBean;
import searchengine.utils.PageData;
import searchengine.utils.SiteWrap;
import searchengine.exceptions.SearchServiceException;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchObject;
import searchengine.dto.search.SearchRequest;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.stream.Collectors;

import static searchengine.utils.UrlUtils.*;
import static searchengine.exceptions.SearchServiceException.getTxtFirstLine;
import static searchengine.dto.search.SearchResponse.*;

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
            List<PageData> pagesData = findPages(lemmaSet, req.getSiteUrl());
            List<PageData> limitedList = getLimitedPagesData(pagesData, req.getOffset(), req.getLimit());
            List<SearchObject> snippedObjectList = getSearchObjects(limitedList, req.getQuery());
            return getSearchRespOk(snippedObjectList);
        } catch (Exception e) {
            return checkingClassOfError(e);
        }
    }

    private SearchResponse checkingClassOfError(Exception e) {
        return e instanceof SearchServiceException ?
                getSearchRespError(getTxtFirstLine(e.getMessage())) :
                getSearchRespError("Ошибка. Class Erorror : " + e.getClass());
    }

    private Set<String> getLemmaSet(String txt) throws Exception {
        Set<String> lemmaNames = lemmaFinderInstance.getLemmaFinder().getLemmaSet(txt);
        chekingForEmpty(lemmaNames, "Передана пустая строка");
        return lemmaNames;
    }

    private List<PageData> findPages(Set<String> lemmaNames, String siteUrl) throws Exception {
        List<SiteEntity> listSites = (siteUrl == null) ?
                siteRepository.findAll() :
                siteRepository.findByUrl(siteUrl);
        chekingForEmpty(listSites, "Индексация не проводилась");
        return getPagesDataBySiteCount(lemmaNames, listSites);
    }

    private void chekingForEmpty(Collection set, String error) throws SearchServiceException {
        if (set.isEmpty()) {
            throw new SearchServiceException(error);
        }
    }

    private List<PageData> getPagesDataBySiteCount(Set<String> lemmaNames, List<SiteEntity> sites) {
        return sites.size() == 1 ?
                new LinkedList<>(createQueryListAndFindPagesData(lemmaNames, sites.get(0))) :
                getPagesDataOfAllSite(lemmaNames, sites);

    }

    private List<PageData> getPagesDataOfAllSite(Set<String> lemmaNames, List<SiteEntity> listSites) {
        List<PageData> pages = new LinkedList<>();
        for (SiteEntity site : listSites) {
            pages.addAll(createQueryListAndFindPagesData(lemmaNames, site));
        }
        return pages;
    }

    private List<PageData> createQueryListAndFindPagesData(Set<String> lemmaNames, SiteEntity site) {
        int countPage = pageRepository.countBySiteId(site.getId());
        List<String> reWritesRanksPagesData = countPage < 10 ?
                new ArrayList<>(lemmaNames) :
                getQueryListByCoeficient(countPage, lemmaNames, site);
        return getRewritePagesDataByRank(reWritesRanksPagesData, site);
    }

    private List<String> getQueryListByCoeficient(int countPage, Set<String> lemmaNames, SiteEntity site) {
        int edgeValue = countPage - countPage / 4;
        return getEdgeRankQueryList(edgeValue, lemmaNames, site);
    }

    private List<String> getEdgeRankQueryList(int edgeValue, Set<String> lemmaNames, SiteEntity site) {
        List<String> queries = createEdgeRankQueryList(edgeValue, lemmaNames, site);
        return queries.isEmpty() ?
                new ArrayList<>(lemmaNames) :
                queries;
    }

    private List<String> createEdgeRankQueryList(int edgeValue, Set<String> lemmaNames, SiteEntity site) {
        List<String> queries = new ArrayList<>(lemmaNames);
        for (String lemma : queries) {
            String lemByonFreq = lemmaRepository.findLemmaByondEdgeFreq(edgeValue, lemma, site.getId());
            if (lemByonFreq != null) {
                queries.remove(lemByonFreq);
            }
        }
        return queries;
    }

    private List<PageData> getRewritePagesDataByRank(List<String> lemmaNames, SiteEntity site) {
        List<LemmaEntity> lemmas = getSortLemmas(lemmaNames, site);
        List<PageData> pagesData = getPagesData(lemmas);
        if (!pagesData.isEmpty()) {
            pagesData = reWriteRanks(pagesData);
        }
        return pagesData;
    }

    private List<PageData> getPagesData(List<LemmaEntity> lemmas) {
        List<PageData> pagesData = new ArrayList<>();
        for (int i = 0; i < lemmas.size(); i++) {
            LemmaEntity lemma = lemmas.get(i);
            pagesData = (i == 0) ?
                    createPagesDataByPagesId(lemma.getId()) :
                    filterPagesData(pagesData, lemma.getId());
        }
        return pagesData;
    }

    private List<PageData> createPagesDataByPagesId(int lemmaId) {
        List<PageData> pagesData = new ArrayList<>();
        List<Integer> pagesIds = indexRepository.findPageIdByLemmaId(lemmaId);
        return createPagesData(lemmaId, pagesIds, pagesData);
    }

    private List<PageData> createPagesData(int lemmaId, List<Integer> pagesId, List<PageData> pagesData) {
        for (Integer pageId : pagesId) {
            List<Float> listRank = indexRepository.findRankByLemmaIdAndPageId(lemmaId, pageId);
            if (!listRank.isEmpty()) {
                Float rank = listRank.get(0);
                pagesData.add(new PageData(pageId, rank));
            }
        }
        return pagesData;
    }

    private List<PageData> filterPagesData(List<PageData> pagesData, int lemmaId) {
        for (PageData pageData : pagesData) {
            List<Integer> lemmaList = indexRepository.findLemmaIdByPageId(pageData.getPageId());
            if (!lemmaList.contains(lemmaId)) {
                pagesData.remove(pageData);
                continue;
            }
            findAndSetPageDataGeneralRank(lemmaId, pageData);
        }
        return pagesData;
    }

    private void findAndSetPageDataGeneralRank(int lemmaId, PageData pageData) {
        List<Float> list = indexRepository.findRankByLemmaIdAndPageId(lemmaId, pageData.getPageId());
        if (!list.isEmpty()) {
            Float rank = list.get(0);
            Float generalRenk = pageData.getGeneralRank();
            pageData.setGeneralRank(generalRenk + rank);
        }
    }

    private List<PageData> reWriteRanks(List<PageData> pagesData) {
        float allRank = getGeneralRankAllSites(pagesData);
        return writeRanks(pagesData, allRank);
    }

    private float getGeneralRankAllSites(List<PageData> pagesData) {
        float allRank = pagesData.stream()
                .map(PageData::getGeneralRank)
                .reduce(Float::sum)
                .get();
        if (allRank == 0) {
            allRank = 1;
        }
        return allRank;
    }

    private List<PageData> writeRanks(List<PageData> pagesData, float allRank) {
        for (PageData pageData : pagesData) {
            float newRank = pageData.getGeneralRank() / allRank;
            pageData.setGeneralRank(newRank);
        }
        pagesData.sort(PageData.generalRankComparator);
        return pagesData;
    }

    private List<LemmaEntity> getSortLemmas(List<String> lemmaNames, SiteEntity site) {
        Map<String, LemmaEntity> lemmasMap = getLemmasMap(site);
        List<LemmaEntity> lemmas = getLemmasFromLemmaNamesList(lemmasMap, lemmaNames);
        lemmas.sort(LemmaEntity.frequencyComparator);
        return lemmas;
    }

    private Map<String, LemmaEntity> getLemmasMap(SiteEntity site) {
        SiteWrap siteWrap = siteMapBean.getSiteMap().get(site.getUrl());
        if (siteWrap == null) {
            return new HashMap<>();
        }
        Map<String, LemmaEntity> lemmasMap = siteMapBean.getSiteMap().get(site.getUrl()).getLemmaMap();
        if (lemmasMap.keySet().isEmpty()) {
            return new HashMap<>();
        }
        return lemmasMap;
    }

    private List<LemmaEntity> getLemmasFromLemmaNamesList(Map<String, LemmaEntity> lemmasMap, List<String> lemmaNames) {
        List<LemmaEntity> lemmaList = new ArrayList<>();
        for (String lemmaName : lemmaNames) {
            if (lemmasMap.containsKey(lemmaName)) {
                LemmaEntity lemma = lemmasMap.get(lemmaName);
                lemmaList.add(lemma);
            }
        }
        return lemmaList;
    }

    private List<SearchObject> getSearchObjects(List<PageData> pagesData, String requestQuery) {
        List<SearchObject> searchObjects = new LinkedList<>();
        for (PageData pageData : pagesData) {
            Optional<PageEntity> pageOpt = pageRepository.findById(pageData.getPageId());
            pageOpt.ifPresent(pageEntity -> createAndAddListSnippedObjects(pageEntity, requestQuery, searchObjects));
        }
        return searchObjects;
    }

    private void createAndAddListSnippedObjects(PageEntity p, String requestQuery, List<SearchObject> searchObjects) {
        String snipped = getSnipped(p.getContent(), requestQuery);
        searchObjects.add(new SearchObject(
                        cleanSlashUrl(p.getSite().getUrl())
                        , p.getSite().getName()
                        , p.getPath()
                        , getTitle(p.getContent())
                        , snipped
                )
        );
    }

    private String getTitle(String str) {
        Document doc = Jsoup.parse(str);
        Elements elementsHead = doc.select("head >title");
        return elementsHead.text();
    }

    private List<PageData> getLimitedPagesData(List<PageData> pagesData, int offset, int limit) {
        List<PageData> limitedPagesData = new ArrayList<>();
        if (offset > pagesData.size()) {
            offset = 0;
        }
        limit = Math.min(offset + limit, pagesData.size());
        for (; offset < limit; offset++) {
            limitedPagesData.add(pagesData.get(offset));
        }
        return limitedPagesData;
    }

    private String getSnipped(String txt, String query) {
        StringBuilder build = new StringBuilder();
        int start = 0;
        int stop = txt.length();
        int entryPoint = txt.indexOf(query);
        int exitPoint = entryPoint + query.length() - 1;
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


