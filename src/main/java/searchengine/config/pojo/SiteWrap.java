package searchengine.config.pojo;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.AllRepAndLemmaInstance;

import searchengine.config.pojo.tasks.ParserRecursive;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;

import static searchengine.config.pojo.StaticMetods.*;

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

    public SiteWrap(TasksStopController stopController, AllRepAndLemmaInstance allRep, SiteEntity site) {
        this.lemmaRepository = allRep.getRepJpaLemma();
        this.siteRepository = allRep.getRepJpaSite();
        this.indexRepository = allRep.getRepJpaIndex();
        this.pageRepository = allRep.getRepJpaPage();
        this.lemmaFinder = allRep.getLemmaFinderInstance().getLemmaFinder();
        this.stopController = stopController;
        this.domUrl = cleanSlashUrl(cleanWwwUrl(site.getUrl()));
        this.site = site;
    }

    public Set<String> getUrlsFromContext(Elements elements) {
        Set<String> urlSet = new HashSet<>();
        for (Element el : elements) {
            String attrHref = el.attr("href");
            if (attrHref.equals("")) {
                continue;
            }
            if (urlsFiltr(domUrl, attrHref)) {
                urlSet.add(attrHref);
            }
            if (attrHref.charAt(0) == '/' && getFiltr(attrHref)) {
                StringBuilder URLendHtml = new StringBuilder(domUrl);
                URLendHtml.append(attrHref);
                urlSet.add(URLendHtml.toString());
            }
        }
        return urlSet;
    }

    public void taskFJP(Connection.Response response) throws Exception {
        LinkedList<ParserRecursive> tasks = new LinkedList<>();
        Document doc = response.parse();
        Elements elements = doc.select("a");
        Set<String> urlSet = getUrlsFromContext(elements);
        for (String u : urlSet) {
            if (stopController.isStop()) {
                return;
            }
            containsUrlList(u, tasks);
        }
        for (ParserRecursive task : tasks) {
            task.join();
        }
    }

    public void savePage(PageEntity page) {
        pageRepository.save(page);
        site.setTimeNow();
        siteRepository.save(site);
    }

    public void createStatusErrorPage(String path, int statusCode) {
        String childPath = parseUrlChild(path);
        PageEntity page = new PageEntity(site, childPath, statusCode, "");
        savePage(page);
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

    public void pushQueryCreatLemmaAndInd(String txt, PageEntity page) {
        Map<String, Integer> mapLem = lemmaFinder.collectLemmas(txt);
        for (String lemmaKey : mapLem.keySet()) {
            if (stopController.isStop()) {
                return;
            }
            synchronized (this) {
                boolean value = lemmaMap.keySet().contains(lemmaKey);
                LemmaEntity lemma;
                if (value) {
                    lemma = lemmaMap.get(lemmaKey);
                    lemma.setFrequency(lemma.getFrequency() + 1);
                } else {
                    lemma = new LemmaEntity(site, lemmaKey);
                    lemmaMap.put(lemmaKey, lemma);
                }
                lemmaRepository.save(lemma);
                IndexEntity index = new IndexEntity(page, lemma, mapLem.get(lemmaKey));
                indexRepository.save(index);
            }
        }
    }

    public synchronized void containsUrlList(String path, List<ParserRecursive> list) {
        if (urlSet.contains(path)) {
            return;
        }
        urlSet.add(path);
        ParserRecursive task = new ParserRecursive(path, this);
        task.fork();
        list.add(task);
    }
}
