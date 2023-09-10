package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.LemmaFinderInstance;
import searchengine.config.SiteMapBean;
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
import java.util.stream.Collectors;

import static searchengine.config.pojo.StaticMetods.*;


@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SiteMapBean siteMapBean;
    private final LemmaFinderInstance lemmaFinderInstance;
    private final PageRepository repositoryJpaPage;
    private final IndexRepository repositoryJpaIndex;
    private final LemmaRepository repositoryJpaLemma;
    private final SiteRepository repositoryJpaSite;

    @Override
    public SearchResponse search(SearchRequest request) {
        Set<String> lemmaSet = lemmaFinderInstance.getLemmaFinder().getLemmaSet(request.getQuery());
        if (lemmaSet.isEmpty()) {
            return getSearchresponsError("Передана пустая строка");
        }
        List<PageEntity> pageList = new LinkedList<>();
        if (request.getSiteUrl() != null) {
            List<SiteEntity> listSite = repositoryJpaSite.findByUrl(request.getSiteUrl());
            if (listSite.isEmpty()) {
                return new SearchResponse("Сайт не индексирован");
            }
            pageList.addAll(creatQueryAndFindPage(lemmaSet, listSite.get(0), pageList));
        } else {
            List<SiteEntity> listSites = repositoryJpaSite.findAll();
            if (listSites.isEmpty()) {
                return new SearchResponse("Сайты не идексированы");
            }
            for (SiteEntity site : listSites) {
                pageList.addAll(creatQueryAndFindPage(lemmaSet, site, pageList));
            }
        }
        List<SnippedObject> snippedObjectList = getListSnippetObject(pageList, request.getQuery());
        snippedObjectList = getLimitList(snippedObjectList, request.getOffset(), request.getLimit());
        return getSearchRespOk(snippedObjectList);
    }


    public List<PageEntity> creatQueryAndFindPage(Set<String> lemmasNames, SiteEntity site, List<PageEntity> pageList) {
        int countPage = repositoryJpaPage.countBySiteId(site.getId());
        if (countPage < 10) {
            return findPage(new ArrayList<>(lemmasNames), site, pageList);
        }
        int edgeValue = countPage - countPage / 4;
        List<String> creatLemmQuery = creatQuery(edgeValue, lemmasNames, site);
        return findPage(creatLemmQuery, site, pageList);
    }

    public List<String> creatQuery(int edgeValue, Set<String> lemmasNames, SiteEntity site) {
        List<String> creatLemmQuery = new ArrayList<>(lemmasNames);
        for (String lemma : creatLemmQuery) {
            String lemByonFreq = repositoryJpaLemma.findLemmaByondEdgeFreq(edgeValue, lemma, site.getId());
            if (lemByonFreq != null) {
                creatLemmQuery.remove(lemByonFreq);
            }
        }
        if (creatLemmQuery.isEmpty()) {
            return new ArrayList<>(lemmasNames);
        }
        return creatLemmQuery;
    }

    public List<PageEntity> findPage(List<String> lemmaNames, SiteEntity site, List<PageEntity> pageList) {
        List<LemmaEntity> lemmas = getSortLemmas(lemmaNames, site);
        for (int i = 0; i < lemmas.size(); i++) {
            LemmaEntity lemma = lemmas.get(i);
            String lemmaName = lemma.getLemma();
            if (i == 0) {
                List<IndexEntity> indexsList = repositoryJpaIndex.findByLemmaId(lemma.getId());
                pageList = indexsList.stream().map(index -> index.getPage()).collect(Collectors.toList());
            }
            if (i > 0) {
                pageList = filterPageList(pageList, lemmaName);
            }
        }
        return pageList;
    }

    public List<PageEntity> filterPageList(List<PageEntity> pageList, String lemmaName) {
        return pageList.stream()
                .filter(p -> p.getIndexSet().stream().anyMatch(is -> is.getLemma().getLemma().equals(lemmaName)
                ))
                .collect(Collectors.toList());
    }

    public List<LemmaEntity> getSortLemmas(List<String> lemmaNames, SiteEntity site) {
        Map<String, LemmaEntity> lemmasMap = siteMapBean.getSiteMap().get(site.getUrl()).getLemmaMap();
        List<LemmaEntity> lemmas = getLemmasFromNamesList(lemmasMap, lemmaNames);
        Collections.sort(lemmas, LemmaEntity.getFrequencyComparator());
        return lemmas;
    }

    public List<LemmaEntity> getLemmasFromNamesList(Map<String, LemmaEntity> lemmasMap, List<String> lemmaNames) {
        List<LemmaEntity> lemmaList = new ArrayList<>();
        for (String lemmaName : lemmaNames) {
            LemmaEntity lemma = lemmasMap.get(lemmaName);
            if (lemma != null) {
                lemmaList.add(lemma);
            }
        }
        return lemmaList;
    }

    public List<SnippedObject> getListSnippetObject(List<PageEntity> pageList, String requestQuery) {
        List<SnippedObject> snObList = new ArrayList<>();
        for (PageEntity p : pageList) {
            snObList.add(new SnippedObject(
                    cleanSlashUrl(p.getSite().getUrl())
                    , p.getSite().getName()
                    , p.getPath()
                    , getTitle(p.getContent())
                    , getSnipped(p.getContent(), requestQuery)
                    , getSummRankPage(p) / getSummRankPages(pageList))
            );
        }
        return snObList;
    }

    public float getSummRankPages(List<PageEntity> listPage) {
        return listPage.stream()
                .map(p -> p.getIndexSet().stream().map(index -> index.getRank()).reduce((x, y) -> x + y).get())
                .reduce((x, y) -> x + y).get();
    }

    public float getSummRankPage(PageEntity page) {
        return page.getIndexSet().stream().map(ind -> ind.getRank()).reduce((x, y) -> x + y).get();
    }

    public String getTitle(String str) {
        Document doc = Jsoup.parse(str);
        Elements elementsHead = doc.select("head >title");
        return elementsHead.text();
    }

    public List<SnippedObject> getLimitList(List<SnippedObject> snOb, int offset, int limit) {
        List<SnippedObject> l = new ArrayList<>();
        int start = 0;
        if (offset < snOb.size()) {
            start = offset;
        }
        int end = (offset + limit > snOb.size()) ? snOb.size() : offset + limit;
        for (; start < end; start++) {
            l.add(snOb.get(start));
        }
        Collections.sort(l);
        return l;
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
