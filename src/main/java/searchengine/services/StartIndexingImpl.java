package searchengine.services;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.exeptionClass.ExceedingNumberPages;
import searchengine.compAndPojoClass.Indexing;
import searchengine.compAndPojoClass.LemmaFinder;
import searchengine.compAndPojoClass.SiteConf;
import searchengine.config.*;
import searchengine.model.*;
import searchengine.repository.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Data
@Service
public class StartIndexingImpl implements StartIndexing {
    private LocalDateTime startTime;
    private static ForkJoinPool example = new ForkJoinPool(15);
    public Map<String, StopControllerWokerTask.Wrap> pageMap = new HashMap<>();
    private final SitesList sites;
    private StopControllerWokerTask stopCont;
    @Autowired
    private Indexing indexing;
    @Autowired
    private RepJpaSite repJpaSite;
    @Autowired
    private RepJpaIndex repJpaIndex;
    @Autowired
    private RepJpaLemma repJpaLemma;
    @Autowired
    private RepJpaPage repJpaPage;

    public static Connection.Response connection(String path) throws IOException {
        String[] refers = {"http://www.yahoo.com", "http://www.vk.ru"};
        String[] user = {"SearchBot", "Mozilla"};
        String znach = Math.random() * 10 > 5 ? refers[0] : refers[1];
        String usera = Math.random() * 10 > 5 ? user[0] : user[1];
        return HttpConnection.connect(path)
                .timeout(10000)
                .userAgent(usera)
                .referrer(znach)
                .execute();
    }

    public static Integer parseStatus(String txt) {
        int v = txt.indexOf("us=");
        String tx = txt.substring(v + 3, v + 6);
        return Integer.parseInt(tx);
    }

    public boolean getFiltr(String attrHref) {
        Boolean endWith =
                !attrHref.endsWith(".pdf")
                        && !attrHref.endsWith(".php")
                        && !attrHref.endsWith(".jpg")
                        && !attrHref.endsWith(".png");
        return endWith;
    }

    public static String cleanSlashUrl(String path) {
        StringBuilder builder = new StringBuilder(path);
        if (builder.charAt(builder.length() - 1) == '/') {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    public String cleanWwwUrl(String url) {
        StringBuilder shortUrl = new StringBuilder();
        String[] txt = url.split("/");
        String[] domainName = txt[2].split("\\.");
        if (domainName.length < 3) {
            return url;
        }
        shortUrl.append(txt[0]);
        shortUrl.append("/");
        shortUrl.append("/");
        shortUrl.append(domainName[1]);
        shortUrl.append(".");
        shortUrl.append(domainName[2]);
        return shortUrl.toString();
    }

    public static String parseUrlChild(String path) {
        String[] ptx = path.split("/");
        StringBuilder childPath = new StringBuilder("/");
        if (ptx.length == 3) {
            return childPath.toString();
        }
        String[] ptxt = path.split("/", 4);
        return childPath.append(ptxt[3]).toString();
    }

    public boolean addListUrlFiltr(String domUrl, String attrHref) {
        return (attrHref.contains(domUrl))
                || attrHref.contains(cleanWwwUrl(domUrl))
                && getFiltr(attrHref)
                && attrHref.indexOf("http") == 0;
    }

    public class StopControllerWokerTask {
        volatile boolean stop = false;

        public class ParserRecursiveSitesList extends RecursiveAction {
            private TreeSet<SiteConf> sites;
            private volatile boolean stop = false;

            public ParserRecursiveSitesList(SitesList list) {
                sites = list.getSites();
            }

            @Override
            protected void compute() {
                LinkedList<Wrap.ParserRecursiveMain> list = new LinkedList<>();
                for (SiteConf s : sites) {
                    try {
                        Connection.Response resp = connection(s.getUrl());
                        if (resp.bodyAsBytes().length == 0) {
                            trowIoException(s, "Главная страница сайта недоступна");
                            continue;
                        }
                        creatAndPushTask(s, resp, list);
                    } catch (IOException e) {
                        trowIoException(s, "Главная страница сайта недоступна");
                    } catch (Exception e) {
                    }
                }
                for (Wrap.ParserRecursiveMain task : list) {
                    task.fork();
                }
                for (Wrap.ParserRecursiveMain task : list) {
                    task.join();
                }
            }

            public void creatAndPushTask(SiteConf s, Connection.Response resp, List<Wrap.ParserRecursiveMain> list) {
                if (stop) {
                    Site site = new Site(s.getName(), s.getUrl(), StatusType.FAILED, "Индексация прервана пользователем");
                    repJpaSite.save(site);
                    return;
                }
                customDeleteSiteByUrl(s.getUrl());
                if (pageMap.containsKey(s.getUrl())) {
                    pageMap.remove(s.getUrl());
                }
                Site site = new Site(s.getName(), s.getUrl(), StatusType.INDEXING);
                repJpaSite.save(site);
                Wrap wrap = new Wrap(site);
                try {
                    wrap.lemmaFinder = LemmaFinder.getInstance();
                } catch (IOException e) {
                }
                pageMap.put(s.getUrl(), wrap);
                Wrap.ParserRecursiveMain parserTask =
                        wrap.new ParserRecursiveMain(cleanSlashUrl(s.getUrl())
                                , cleanSlashUrl(s.getUrl())
                                , site
                                , wrap.createPage(s.getUrl(), site));
                parserTask.setResponse(resp);
                list.add(parserTask);
            }
        }

        @Setter
        @Getter
        public abstract class RecursiveTaskMain extends ForkJoinTask<Void> {
            private static final long serialVersionUID = 5232453952276485070L;
            protected Site site;
            protected Page page;
            protected Connection.Response response;
            protected String path;

            public RecursiveTaskMain() {
            }

            protected abstract void compute() throws Exception;

            public final Void getRawResult() {
                return null;
            }

            protected final void setRawResult(Void mustBeNull) {
            }

            protected final boolean exec() {
                if (stop) {
                    return true;
                }
                try {
                    this.compute();
                    if (stop) {
                        setFailedCancelled(site);
                        return true;
                    } else {
                        setIndexed(site);
                    }
                    return true;
                } catch (Exception e) {
                    return true;
                }
            }
        }

        public abstract class RecursiveActionSecond extends ForkJoinTask<Void> {
            private static final long serialVersionUID = 5232453952276485070L;
            protected Site site;
            protected Page page;
            protected Connection.Response response;
            protected String path;

            public RecursiveActionSecond() {
            }

            protected abstract void compute() throws Exception;

            public final Void getRawResult() {
                return null;
            }

            protected final void setRawResult(Void mustBeNull) {
            }

            protected final boolean exec() {
                if (stop) {
                    return true;
                }
                try {
                    this.compute();
                    return true;
                } catch (IOException e) {
                    if (e instanceof HttpStatusException) {
                        page.setCode(parseStatus(e.getMessage()));
                        repJpaPage.save(page);
                        return true;
                    }
                    return true;
                } catch (Exception e) {
                    return true;
                }
            }
        }

        @Getter
        @Setter
        public class Wrap {
            private LemmaFinder lemmaFinder;
            public Set<String> pageSett = new HashSet<>();
            private Site site;

            public Wrap(Site site) {
                this.site = site;
            }

            public class ParserRecursiveMain extends RecursiveTaskMain {
                private String domUrl;
                private Set<String> listUrl = new TreeSet<>();
                private LinkedList<ParserRecursive> tasks = new LinkedList<>();

                public ParserRecursiveMain(
                        String path,
                        String domUrl,
                        Site site,
                        Page page
                ) {
                    this.path = path;
                    this.site = site;
                    this.domUrl = domUrl;
                    this.page = page;
                }

                @Override
                protected void compute() throws Exception {
                    andUpdatePage(page, site, response);
                    taskFJP(response, site);
                }

                public void taskFJP(Connection.Response response, Site site) throws Exception {
                    Document doc = response.parse();
                    Elements elements = doc.select("a");
                    for (Element el : elements) {
                        String attrHref = el.attr("href");
                        if (attrHref.equals("")) {
                            continue;
                        }
                        if (addListUrlFiltr(domUrl, attrHref)) {
                            listUrl.add(attrHref);
                        }
                        if (attrHref.charAt(0) == '/' && getFiltr(attrHref)) {
                            StringBuilder URLendHtml = new StringBuilder(domUrl);
                            URLendHtml.append(attrHref);
                            listUrl.add(URLendHtml.toString());
                        }
                    }
                    for (String u : listUrl) {
                        if (stop) {
                            break;
                        }
                        containsUrlList(u, domUrl, site, tasks);
                    }
                    for (ParserRecursive task : tasks) {
                        task.join();
                    }
                }
            }

            @Setter
            public class ParserRecursive extends RecursiveActionSecond {
                private String domUrl;
                private Set<String> listUrl = new TreeSet<>();
                private LinkedList<ParserRecursive> tasks = new LinkedList<>();

                public ParserRecursive(
                        String path,
                        String domUrl,
                        Site site,
                        Page page
                ) {
                    this.path = path;
                    this.site = site;
                    this.domUrl = domUrl;
                    this.page = page;
                }

                @Override
                protected void compute() throws Exception {
                    response = connection(path);
                    andUpdatePage(page, site, response);
                    taskFJP(response, site);
                }

                public void taskFJP(Connection.Response response, Site site) throws Exception {
                    Document doc = response.parse();
                    Elements elements = doc.select("a");
                    for (Element el : elements) {
                        String attrHref = el.attr("href");
                        if (attrHref.equals("")) {
                            continue;
                        }
                        if (addListUrlFiltr(domUrl, attrHref)) {
                            listUrl.add(attrHref);
                        }
                        if (attrHref.charAt(0) == '/' && getFiltr(attrHref)) {
                            StringBuilder URLendHtml = new StringBuilder(domUrl);
                            URLendHtml.append(attrHref);
                            listUrl.add(URLendHtml.toString());
                        }
                    }
                    for (String u : listUrl) {
                        if (stop) {
                            return;
                        }
                        containsUrlList(u, domUrl, site, tasks);
                    }
                    for (ParserRecursive task : tasks) {
                        task.join();
                    }
                }
            }

            public synchronized void andUpdatePage(Page page, Site site, Connection.Response response) throws ExceedingNumberPages {
                String txt = response.body();
                page.setCode(response.statusCode());
                page.setContent(txt);
                repJpaPage.save(page);
                pageSett.add(page.getPath());
                if (response.statusCode() < 400) {
                    pushQueryCreatLemmaAndInd(txt, site, page);
                }
            }

            public synchronized void pushQueryCreatLemmaAndInd(String txt, Site site, Page page) throws ExceedingNumberPages {
                Map<String, Integer> mapLem = lemmaFinder.collectLemmas(txt);
                for (String lemmaKey : mapLem.keySet()) {
                    Optional<Lemma> optL = site.getLemmaSet().stream().filter(l -> l.getLemma().equals(lemmaKey)).findFirst();
                    Lemma l;
                    if (optL.isPresent()) {
                        l = optL.get();
                        l.setFrequency(optL.get().getFrequency() + 1);
                    } else {
                        l = new Lemma(site, lemmaKey, site.getPageSet().size());
                        site.getLemmaSet().add(l);
                        repJpaLemma.save(l);
                    }
                    Index index = new Index(page, l, mapLem.get(lemmaKey));
                    repJpaIndex.save(index);
                }
            }

            public synchronized Page createPage(String childPath, Site site) {
                Page page = new Page(site, childPath, -1, "");
                repJpaPage.save(page);
                site.setTimeNow();
                repJpaSite.save(site);
                return page;
            }

            public synchronized boolean findPage(String path) {
                if (pageSett.contains(path)) {
                    return true;
                }
                return false;
            }

            public synchronized void containsUrlList(String path, String domUrl, Site site, List<ParserRecursive> list) {
                String childPath = parseUrlChild(path);
                if (findPage(path)) {
                    return;
                }
                Page page = createPage(childPath, site);
                pageSett.add(path);
                ParserRecursive task =
                        new ParserRecursive(path, domUrl, site, page);
                task.fork();
                list.add(task);
            }
        }
    }

    @Override
    public void startIndexing() {
        indexing.setIndexing(true);
        stopCont = new StopControllerWokerTask();
        StopControllerWokerTask.ParserRecursiveSitesList task = stopCont.new ParserRecursiveSitesList(sites);
        example.invoke(task);

    }

    public void trowIoException(SiteConf s, String ex) {
        List<Site> lsite = repJpaSite.findByUrl(s.getUrl());
        if (!(lsite).isEmpty()) {
            Site site = lsite.get(0);
            site.setTimeNow();
            site.setLastError(ex);
            repJpaSite.save(site);
        } else {
            Site site = new Site(s.getName(), s.getUrl(), StatusType.FAILED, ex);
            repJpaSite.save(site);
        }
    }

    public void customDeleteSiteByUrl(String siteUrl) {
        List<Site> lsite = repJpaSite.findByUrl(siteUrl);
        if (!lsite.isEmpty()) {
            repJpaSite.deleteById(lsite.get(0).getId());
        }
    }

    public void setIndexed(Site site) {
        site.setStatus(StatusType.INDEXED);
        repJpaSite.save(site);
    }

    public void setFailedCancelled(Site site) {
        site.setStatus(StatusType.FAILED);
        site.setLastError("Индексация прервана пользователем");
        repJpaSite.save(site);
    }

    @Override
    public void stopIndexing() {
        stopCont.stop = true;
        indexing.setIndexing(false);
    }
}
