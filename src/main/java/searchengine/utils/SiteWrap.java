package searchengine.utils;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.AllRepAndLemmaInstance;
import searchengine.config.ConnectionMetod;
import searchengine.utils.tasks.ParserRecursive;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;

import static searchengine.utils.UrlUtils.*;

@Getter
@Setter
public class SiteWrap {
    private String domUrl;
    private LemmaFinder lemmaFinder;
    private Set<String> urlSet = new HashSet<>();
    private Map<String, LemmaEntity> lemmaMap = new HashMap<>();
    private volatile TasksStopController stopController;
    private SiteEntity site;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final ConnectionMetod connectionMetod;

    public SiteWrap(TasksStopController stopController, AllRepAndLemmaInstance allRep, SiteEntity site) {
        this.connectionMetod = allRep.getConnectionMetod();
        this.lemmaRepository = allRep.getRepJpaLemma();
        this.siteRepository = allRep.getRepJpaSite();
        this.indexRepository = allRep.getRepJpaIndex();
        this.pageRepository = allRep.getRepJpaPage();
        this.lemmaFinder = allRep.getLemmaFinderInstance().getLemmaFinder();
        this.stopController = stopController;
        this.domUrl = cleanSlashUrl(cleanWwwUrl(site.getUrl()));
        this.site = site;
    }

    public void taskFJP(Connection.Response response) throws Exception {
        Document doc = response.parse();
        Elements elements = doc.select("a");
        Set<String> foundSetUrl = getUrlsFromContext(elements);
        createAndPushTasks(foundSetUrl);
    }

    private Set<String> getUrlsFromContext(Elements elements) {
        Set<String> foundSetUrl = new HashSet<>();
        for (Element el : elements) {
            String attrHref = el.attr("href");
            if (attrHref.equals("")) {
                continue;
            }
            if (urlsFiltrByDomainPresent(domUrl, attrHref)) {
                foundSetUrl.add(attrHref);
                continue;
            }
            if (urlFilterByNotDomain(attrHref)) {
                foundSetUrl.add(domUrl + attrHref);
            }
        }
        return foundSetUrl;
    }

    private void createAndPushTasks(Set<String> foundSetUrl) {
        LinkedList<ParserRecursive> tasks = new LinkedList<>();
        for (String u : foundSetUrl) {
            containsUrlList(u, tasks);
        }
        for (ParserRecursive task : tasks) {
            task.join();
        }
    }

    public void createPage(String path, Connection.Response response) {
        String childPath = parseUrlChild(path);
        String txt = response.body();
        int statusCode = response.statusCode();
        PageEntity page = new PageEntity(site, childPath, statusCode, txt);
        savePage(page);
        if (response.statusCode() < 400) {
            pushQueryCreatLemmaAndInd(txt, page);
        }
    }

    public void createStatusErrorPage(String path, int statusCode) {
        String childPath = parseUrlChild(path);
        PageEntity page = new PageEntity(site, childPath, statusCode, "");
        savePage(page);
    }

    public void savePage(PageEntity page) {
        pageRepository.save(page);
        site.setTimeNow();
        siteRepository.save(site);
    }

    private void pushQueryCreatLemmaAndInd(String txt, PageEntity page) {
        Map<String, Integer> mapLem = lemmaFinder.collectLemmas(txt);
        for (String lemmaKey : mapLem.keySet()) {
            if (stopController.isStop()) {
                return;
            }
            synchronized (this) {
                boolean value = lemmaMap.containsKey(lemmaKey);
                LemmaEntity lemma = value ?
                        setFrequencyLemma(lemmaKey) :
                        addMapNewLemma(lemmaKey);
                lemmaRepository.save(lemma);
                IndexEntity index = new IndexEntity(page, lemma, mapLem.get(lemmaKey));
                indexRepository.save(index);
            }
        }
    }

    private LemmaEntity setFrequencyLemma(String lemmaKey) {
        LemmaEntity lemma = lemmaMap.get(lemmaKey);
        lemma.setFrequency(lemma.getFrequency() + 1);
        return lemma;
    }

    private LemmaEntity addMapNewLemma(String lemmaKey) {
        LemmaEntity lemma = new LemmaEntity(site, lemmaKey);
        lemmaMap.put(lemmaKey, lemma);
        return lemma;
    }

    public synchronized void containsUrlList(String path, List<ParserRecursive> list) {
        if (stopController.isStop() || urlSet.contains(path)) {
            return;
        }
        urlSet.add(path);
        ParserRecursive task = new ParserRecursive(path, this, connectionMetod);
        task.fork();
        list.add(task);
    }
}
