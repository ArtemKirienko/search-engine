package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.compAndPojoClass.LemmaFinder;
import searchengine.compAndPojoClass.SiteConf;
import searchengine.config.SitesList;
import searchengine.dto.search.SnippedObject;


import searchengine.dto.search.RequestObj;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.RepJpaIndex;
import searchengine.repository.RepJpaLemma;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SitesList sites;
    @Autowired
    RepJpaIndex repositoryJpaIndex;
    @Autowired
    RepJpaLemma repositoryJpaLemma;

    @Override
    public ResponseEntity search(RequestObj obj) {
        Set<String> lemmaSet = new HashSet<>();
        try {
            lemmaSet = LemmaFinder.getInstance().getLemmaSet(obj.getQuery());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Set<Lemma> freqLemma = findMaxFreqLem(0).stream().collect(Collectors.toSet());
        lemmaSet.stream().filter(l -> !freqLemma.contains(l)).collect(Collectors.toList());
        List<Page> ps = new ArrayList<>();
        if (obj.getSite() != null) {
            ps = getPageLike(lemmaSet, obj.getSite());
        } else {
            for (SiteConf s : sites.getSites()) {
                ps.addAll(getPageLike(lemmaSet, s.getUrl()));
            }
            System.out.println(ps.size());
        }
        float max = ps.stream().map(
                p -> p.getIndexSet().stream().map(index -> index.getRank())
                        .reduce((x, y) -> x + y).get()
        ).max(Float::compare).get();
        List<SnippedObject> sObjList = new ArrayList<>();
        ps.stream().map(p -> p.getPath()).forEach(System.out::println);
        ps.stream().forEach(p -> {
            if (p.getContent().contains(obj.getQuery())) {
                sObjList.add(new SnippedObject(
                        StartIndexingImpl.cleanSlashUrl(p.getSite().getUrl())
                        , p.getSite().getName()
                        , p.getPath()
                        , getTitle(p.getContent())
                        , getSnipped(p.getContent(), obj.getQuery())
                        , p.getIndexSet().stream().map(Index::getRank).reduce((x, y) -> x + y).get() / max));
            }
        });
        return ResponseEntity.ok(
                new searchengine.dto.search.ResponseTrue(getLimitList(sObjList, obj.getOffset(), obj.getLimit())));
    }

    public String getTitle(String str) {
        Document doc = Jsoup.parse(str);
        Elements elementsHead = doc.select("head >title");
        return elementsHead.text();
    }

    public Set<Lemma> findMaxFreqLem(int param) {
        List<Integer> freq = repositoryJpaLemma.findByFrequencyOrderByFrequencyDesc();
        if (freq.isEmpty()) {
            return new HashSet<>();
        }
        Set<Lemma> lLemma = repositoryJpaLemma.getLemmaSort(freq.get(0), param);
        if (lLemma.isEmpty()) {
            return new HashSet<>();
        }
        return lLemma;
    }

    public List<Page> getPageLike(Set<String> lemmaSet, String siteUrl) {//
        if (lemmaSet.isEmpty()) {
            return new ArrayList<>();
        }
        Iterator<String> iter = lemmaSet.iterator();
        List<Page> pageList = new ArrayList<>();
        int i = 0;
        while (iter.hasNext()) {
            String value = iter.next();
            if (i == 0) {
                List<Index> indexList = repositoryJpaIndex.selectIndexWereLemma(value, siteUrl);
                pageList = indexList.stream().map(in -> in.getPage()).collect(Collectors.toList());
            }
            if (i > 0) {
                pageList.stream().filter(p -> p.getIndexSet().stream().anyMatch(is -> is.getLemma().equals(value))).collect(Collectors.toList());
            }
            i++;
        }
        return pageList;
    }

    public List<SnippedObject> getLimitList(List<SnippedObject> sb, int offset, int limit) {
        List<SnippedObject> l = new ArrayList<>();
        int start;
        if (offset < sb.size()) {
            start = offset;
        } else {
            start = 0;
        }
        int end = (offset + limit > sb.size()) ? sb.size() : offset + limit;
        for (; start < end; start++) {
            l.add(sb.get(start));
        }
        Collections.sort(l);
        l.stream().forEach(System.out::println);
        return l;
    }

    public String getSnipped(String txt, String tx) {
        StringBuilder build = new StringBuilder();
        int start;
        int stop;
        int str = txt.indexOf(tx);
        int fin = txt.indexOf(tx) + (tx.length() - 1);
        if (str < 80) {
            start = 0;
        } else {
            start = str - 80;
        }
        if (txt.length() - fin > 80) {
            stop = fin + 80;
        } else {
            stop = txt.length();
        }
        for (; start < stop; start++) {
            build.append(txt.charAt(start));
            if (start == str - 1) {
                build.append("><b>");
            }
            if (start == fin) {
                build.append("</b><");
            }
        }
        return build.toString();
    }
}
