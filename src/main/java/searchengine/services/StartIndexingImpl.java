package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.bind.DatatypeConverter;

import org.springframework.stereotype.Service;

import searchengine.config.*;

import searchengine.model.*;

import searchengine.repository.Repo;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Setter
@Service
@RequiredArgsConstructor
public class StartIndexingImpl implements StartIndexing {

    private LocalDateTime startTime;

    private ForkJoinPool example = new ForkJoinPool();
    private Thread one = new Thread();
    private volatile Boolean sim = one.isInterrupted();


    //private Set<Page> pageSet = Collections.synchronizedSet(new HashSet<>());
    private Set<Page> pageSet = new HashSet<>();
    private final SitesList sites;
    Object lock = new Object();
    private String domURL;
    private String parseWWWdomURL;

    @Autowired
    private Repo repo;
    @Autowired
    private ControllerStartStop con;


    @Setter
    public class ParserRecursive extends RecursiveAction {
        private String path;
        private Site site;
        private Connection.Response response;
        private String[] refers = {"http://www.google.com", "http://www.yandex.ru"};
        private String[] user = {
                "HeliontSearchBot"
                , "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6"
        };

        public ParserRecursive(String path, Site site) {
            this.path = path;
            this.site = site;

        }


        @Override
        protected void compute() {
            if (sim) {
                return;
            }
            Set<String> listUrl = new TreeSet<>();
            List<ParserRecursive> taskList = new ArrayList<>();


            System.out.println(path + "Заходдддддддддддддддииииииииииииииимммммммммм");

            List<Page> list;
            if (!(list = containsUrlList(path, site)).isEmpty()) {
                Page page = list.get(0);


                try {
                    if (response == null) {
                        response = HttpConnection.connect(path)
                                .userAgent(Math.random() * 10 > 5 ? user[0] : user[1])
                                .referrer(Math.random() * 10 > 5 ? refers[0] : refers[1])
                                .execute();
                    } else {
                        if (response.bodyAsBytes().length == 0) {
                            throw new IOException("Главная страница сайта не доступна");
                        }
                    }


                    andUpdatePage(page, site, response);
                    taskFJP(response, listUrl, taskList, site);


                } catch (HttpStatusException ex) {

                    page.setCode(parseStatus(ex.getMessage()));
                    repo.updateShortPage(page);


                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println(path);
                }


            }

        }

    }

    public synchronized void addPageSet(Page page) {
        pageSet.add(page);
    }

    public List<Page> findPage(Set<Page> set, String path, Site site) {
        List<Page> pageList = new ArrayList<>();
        synchronized (set) {
            for (Page p : set) {

                if (p.getPath().equals(parseUrlChild(path))
                        && p.getSite().getUrl().equals(site.getUrl())) {
                    pageList.add(p);
                    return pageList;
                }
            }
            return pageList;
        }
    }


    private synchronized Integer parseStatus(String txt) {
        int v = txt.indexOf("us=");
        String tx = txt.substring(v + 3, v + 6);
        return Integer.parseInt(tx);
    }

    public static synchronized String parseDomUrl(String path) {
        StringBuilder builder = new StringBuilder(path);
        if (builder.charAt(builder.length() - 1) == '/') {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();

    }

    //удаления www из доменного имени
    private synchronized String parseURL(String url) {

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

    public void taskFJP(Connection.Response response
            , Set<String> listUrl
            , List<ParserRecursive> taskList
            , Site site
    ) throws Exception {
        Document doc = response.parse();
        Elements elements = doc.select("a");

        for (Element el : elements) {
            String attrHref = el.attr("href");

            if (attrHref == "") {
                continue;
            }
            Boolean endWith =
                    !attrHref.endsWith(".pdf")
                            && !attrHref.endsWith(".php")
                            && !attrHref.endsWith(".jpg")
                            && !attrHref.endsWith(".png");
            // System.out.println(el.attr("href"));
            if (
                    (attrHref.contains(domURL)
                            || attrHref.contains(parseWWWdomURL))
                            // && endWith
                            && attrHref.indexOf("http") == 0

            ) {
                // System.out.println(attrHref + "атребут добавленный в список");
                listUrl.add(attrHref);
            }

            // System.out.println(attrHref + "    ищем логи");
            if (attrHref.charAt(0) == '/'
                // && endWith
            ) {

                // System.out.println(domURL);
                StringBuilder URLendHtml = new StringBuilder(domURL);
                URLendHtml.append(el.attr("href"));

                listUrl.add(URLendHtml.toString());
                // System.out.println(URLendHtml + "  атрибут составлен из домаин");
            }

        }

        for (String url : listUrl) {


            ParserRecursive task = new ParserRecursive(url, site);
            task.fork();
            taskList.add(task);
        }

        for (ParserRecursive taskToJoin : taskList) {
            taskToJoin.join();
        }

    }

    public synchronized String parseUrlChild(String path) {
        String[] ptx = path.split("/");
        StringBuilder childPath = new StringBuilder("/");

        if (ptx.length == 3) {
            return childPath.toString();
        }
        String[] ptxt = path.split("/", 4);

        return childPath.append(ptxt[3]).toString();

    }

    public synchronized void pushQueryCreatLemmaAndInd(String txt, Site site, Page page) throws Exception {
        StringBuilder builderIns = new StringBuilder();
        StringBuilder builderSel = new StringBuilder("(");
        Map<String, Integer> mapLem = LemmaFinder.getInstance().collectLemmas(txt);


        for (String lemmaKey : mapLem.keySet()) {


            builderIns.append(",(");
            builderIns.append("1");
            builderIns.append(",'");
            builderIns.append(lemmaKey);
            builderIns.append("',");
            builderIns.append(site.getId());
            builderIns.append(")");


            builderSel.append("'");
            builderSel.append(lemmaKey);
            builderSel.append("'");
            builderSel.append(",");


        }
        builderIns.deleteCharAt(0);
        repo.insertLemmas(builderIns.toString());
        builderSel.deleteCharAt(builderSel.length() - 1).append(")");
        StringBuilder builInInd = new StringBuilder();
        for (Lemma l : repo.selectLemmas(builderSel.toString())) {
            builInInd.append("(");
            builInInd.append((int) mapLem.get(l.getLemma()));

            builInInd.append(",");
            builInInd.append(l.getId());
            builInInd.append(",");
            builInInd.append(page.getId());
            builInInd.append("),");
        }
        builInInd.deleteCharAt(builInInd.length() - 1);
        repo.insertIndexes(builInInd.toString());
    }

    public void andUpdatePage(Page page, Site site, Connection.Response response) throws Exception {


        String txt = response.body();

        page.setCode(response.statusCode());
        page.setContent(txt);
        repo.updateShortPage(page);


        if (response.statusCode() < 400) {

            pushQueryCreatLemmaAndInd(txt, site, page);

        }


    }


    public synchronized List<Page> containsUrlList(String url, Site site) {
        List<Page> listpage = new ArrayList<>();


        //if (repo.findEntPageBool(parseUrlChild(url), site)) {
        if (!findPage(pageSet, url, site).isEmpty()) {
            return listpage;
        } else {


            Page page = new Page(
                    site
                    , parseUrlChild(url)
                    , -1
                    , ""
            );
            addPageSet(page);

            repo.creatEnPage(page);
            site.setTimeNow();
            repo.updateShortSite(site);
            // repo.mergeSite(site);
            listpage.add(page);

            return listpage;


        }

    }


    @Override

    public void startIndexing() {

        Runnable task = () -> {
            con.setIndStart(true);

            Thread current = Thread.currentThread();
            setStartTime(LocalDateTime.now());
            System.out.println("поток запустился");


            for (SiteConf s : sites.getSites()) {
                if (current.isInterrupted()) {
                    break;
                }


                domURL = parseDomUrl(s.getUrl());
                parseWWWdomURL = parseURL(domURL);

                try {
                    Connection.Response resp = connection(s.getUrl());

                    repo.delEnSite(s.getUrl());

                    pageSet.stream()
                            .filter(p -> p.getSite().getUrl().equals(s.getUrl()))
                            .collect(Collectors.toSet()).forEach(p -> pageSet.remove(p));


                    Site site = new Site(s.getName(), s.getUrl(), StatusType.INDEXING);
                    repo.creatSite(site);

                    example = new ForkJoinPool();
                    ParserRecursive parserTask = new ParserRecursive(domURL, site);
                    parserTask.setResponse(resp);
                    example.invoke(parserTask);
                    if (current.isInterrupted()) {
                        break;

                    }
                    site.setStatus(StatusType.INDEXED);
                    repo.updateShortSite(site);


                } catch (IOException ex) {
                    List<Site> lsite;
                    if (!(lsite = repo.findEntSite(s.getUrl())).isEmpty()) {

                        Site st = lsite.get(0);

                        st.setTimeNow();
                        st.setLastError(ex.getMessage());

                        repo.updateShortSite(st);


                    } else {
                        Site site = new Site(s.getName(), s.getUrl(), StatusType.FAILED, ex.getMessage());
                        repo.creatSite(site);


                    }


                } catch (Exception e) {
                    e.printStackTrace();

                }

            }
//            one.interrupted();
//
//            con.setIndStart(false);

            System.out.println("выполнение метода прекращено");

        };


        one = new Thread(task);
        one.start();
//

    }


    @Override

    public void stopIndexing() {

        one.interrupt();
        example.shutdownNow();


        Set<Site> aLLSite = new HashSet<>(repo.getAllSite());


        aLLSite.stream()
                .filter(s -> LocalDateTime.parse(s.getStatusTime(), Site.formatter).isBefore(startTime))
                .forEach(s -> {
                    System.out.println(s.getStatusTime());
                    repo.delEnSite(s.getUrl());
                });
        Set<Site> aLLSitee = new HashSet<>(repo.getAllSite());
        pageSet.stream()
                .filter(p -> LocalDateTime.parse(p.getSite().getStatusTime(), Site.formatter).isBefore(startTime))
                .collect(Collectors.toSet())
                .forEach(p -> pageSet.remove(p));


        sites.getSites().stream().filter(s -> !siteInlist(aLLSitee, s))
                .forEach(s ->
                        repo.creatSite(
                                new Site(s.getName()
                                        , s.getUrl()
                                        , StatusType.FAILED
                                        , "Индексация прервана пользователем")));

        con.setIndStart(false);

    }


    public boolean siteInlist(Set<Site> list, SiteConf site) {
        for (Site s : list) {

            if (s.getUrl().equals(site.getUrl())) {
                return true;
            }
        }
        return false;
    }

    @Override

    public boolean addIndexPage(String str) {


        System.out.println(str);
        String string = decodeURL(str);
        System.out.println(string);
        if (string.indexOf("http") != 0) {

            return false;
        }

        Optional<String> siteDom =
                sites.getSites().stream().map(s -> s.getUrl())
                        .filter(u -> parseURL(parseDomUrl(string)).contains(parseURL(parseDomUrl(u)))).findAny();
        System.out.println(siteDom.get());
        if (siteDom.isPresent()) {
            List<Site> list = repo.findEntSite(siteDom.get());
            if (list.isEmpty()) {
                return false;
            }

            List<Page> listp;
            Page page;
            if (!(listp = findPage(pageSet, string, list.get(0))).isEmpty()) {

                page = listp.get(0);
                repo.removePage(page);
                pageSet.remove(page);
            }
            listp = containsUrlList(string, list.get(0));
            page = listp.get(0);
            try {
                Connection.Response response = connection(string);
                andUpdatePage(page, list.get(0), response);
                return true;

            } catch (HttpStatusException ex) {
                page.setCode(parseStatus(ex.getMessage()));
                repo.updateShortPage(page);
                return true;

            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }
        return false;
    }


    public String decodeURL(String string) {
        int in = string.indexOf("http");


        StringBuilder sb = new StringBuilder();

        int count = 3;
        int first = 0;
        int second = 0;
        byte res = 0;

        for (; in < string.length(); in++) {
            char el = string.charAt(in);


            if (count == 2) {
                first = Character.digit(string.charAt(in), 16);
                --count;
                continue;
            }
            if (count == 1) {
                second = Character.digit(string.charAt(in), 16);
                --count;
                continue;
            }
            if (count == 0) {

                res = (byte) ((first << 4) + second);
                count = 3;
                char s = (char) res;
                sb.append(s);


            }
            if (el == '%') {
                --count;
                continue;
            }
            sb.append(el);


        }
        return sb.toString();

    }

    public Connection.Response connection(String path) throws IOException {
        String[] refers = {"http://www.yahoo.com", "http://www.vk.ru"};
        String[] user = {
                "SearchBot"
                , "Mozilla"
        };
        String znach = Math.random() * 10 > 5 ? refers[0] : refers[1];
        String usera = Math.random() * 10 > 5 ? user[0] : user[1];
        System.out.println(znach);
        System.out.println(usera);

        return HttpConnection.connect(path)
                .userAgent(usera)
                .referrer(znach)
                .execute();

    }


}
