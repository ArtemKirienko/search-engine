package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.LemmaFinder;
import searchengine.config.SiteConf;
import searchengine.config.SitesList;
import searchengine.dto.search.SnippedObject;


import searchengine.dto.search.RequestObj;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.Repo;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl  implements SearchService  {


    private final SitesList sites;

    @Autowired
    private Repo repo;


    @Override
    public ResponseEntity

    search(RequestObj obj) {

        Set<String> lemmaSet = new HashSet<>();
        try {
            lemmaSet = LemmaFinder.getInstance().getLemmaSet(obj.getQuery());

        } catch (IOException e) {
            e.printStackTrace();
        }

        Set<Lemma> freqLemma = repo.findMaxFreqLem(0).stream().collect(Collectors.toSet());
        lemmaSet.stream().filter(l -> !freqLemma.contains(l)).collect(Collectors.toList());
        List<Page> ps = new ArrayList<>();
        if (obj.getSiteUrl() != null) {
            ps = repo.getPageLike(lemmaSet, obj.getSiteUrl());

        } else {
            for (SiteConf s : sites.getSites()) {
                ps.addAll(repo.getPageLike(lemmaSet, s.getUrl()));
            }

        }


        float max = ps.stream().map(
                        p -> p.getIndexSet().stream().map(index -> index.getRank())
                                        .reduce((x, y) -> x + y).get()
                ).max(Float::compare).get();

        List<SnippedObject> sObjList = new ArrayList<>();


        ps.stream().forEach(p -> {
            if (p.getContent().contains(obj.getQuery())) {
                sObjList.add(new SnippedObject(
                        StartIndexingImpl.parseDomUrl(p.getSite().getUrl())
                        , p.getSite().getName()
                        , p.getPath()
                        , StartIndexingImpl.parseDomUrl(p.getSite().getUrl()) + p.getPath()
                        , getSnipped(p.getContent(), obj.getQuery())
                        , p.getIndexSet().stream().map(Index::getRank).reduce((x, y) -> x + y).get() / max));
            }
        });




        return ResponseEntity.ok(
                new searchengine.dto.search.ResponseTrue(getLimitList(sObjList, obj.getOffset(), obj.getLimit())));

    }

public List<SnippedObject> getLimitList(List<SnippedObject> sb, int offset, int limit) {
        List<SnippedObject> l = new ArrayList<>();

    int start;
        if(offset < sb.size()){
            start = offset;

        }else {start = 0;
        }

        int end = (offset + limit > sb.size())? sb.size() : offset + limit;

        for(; start < end; start++){
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

        if (str < 150) {
            start = 0;
        } else {
            start = str - 150;
        }
        if (txt.length() - fin > 150) {
            stop = fin + 150;
        } else {
            stop = txt.length();
        }

        for (; start < stop; start++) {


            build.append(txt.charAt(start));
            if (start == str - 1) {
                build.append("<b>");
            }
            if (start == fin) {
                build.append("</b>");
            }

        }

        return build.toString();


    }


}
