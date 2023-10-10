package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.springframework.stereotype.Service;
import searchengine.config.ExecuteIndicator;
import searchengine.utils.*;
import searchengine.config.*;
import searchengine.exceptions.IndexingServiceException;
import searchengine.utils.tasks.ParserRecursiveSitesList;
import searchengine.dto.indexing.IndexingRequest;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
import searchengine.repository.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static searchengine.utils.UrlUtils.*;
import static searchengine.dto.indexing.IndexingResponse.getIndRespError;
import static searchengine.dto.indexing.IndexingResponse.getIndRespOk;

@Getter
@Setter
@RequiredArgsConstructor
@Service
public class IndexingServiceImpl implements IndexingService {
    private final ConnectionMetod cm;
    private final TaskStopControllerBean stopControllerBean;
    private final AllRepAndLemmaInstance allRepAndLemmaInstance;
    private final SitesList sites;
    private final ExecuteIndicator indicator;
    private final SiteRepository repJpaSite;
    private final PageRepository repJpaPage;
    private final SiteMapBean siteMapBean;

    @Override
    public IndexingResponse startIndexing() {
        return indicator.isExec() ?
                getIndRespError("Индексация уже запущена") :
                run();
    }

    private IndexingResponse run() {
        start();
        return getIndRespOk();
    }

    private void start() {
        Thread startIndexingSiteList = new Thread(() -> {
            indicator.setExec(true);
            TasksStopController stopCont = new TasksStopController();
            stopControllerBean.setTasksStopController(stopCont);
            ParserRecursiveSitesList task =
                    new ParserRecursiveSitesList(stopCont, allRepAndLemmaInstance, siteMapBean, sites, indicator);
            ForkJoinPool example = new ForkJoinPool(sites.getSites().size() + 3);
            example.invoke(task);
        });
        startIndexingSiteList.start();
    }

    @Override
    public IndexingResponse stopIndexing() {
        return indicator.isExec() ?
                runStop() :
                getIndRespError("Индексация не запущена");
    }

    private IndexingResponse runStop() {
        stopControllerBean.getTasksStopController().setStop(true);
        indicator.setExec(false);
        return getIndRespOk();
    }

    @Override
    public IndexingResponse addIndexPage(IndexingRequest request) {
        try {
            String url = request.getUrl();
            urlCheckHttp(url);
            Optional<Site> optional = sitePresentCheck(url);
            return optional.isPresent() ?
                    indexPage(optional.get(), url) :
                    getIndRespError("Сайт находиться за пределами индексируемого списка сайтов");
        } catch (Exception e) {
            return errorClassCheck(e);
        }
    }

    private Optional<Site> sitePresentCheck(String url) {
        return sites.getSites().stream()
                .filter(u -> cleanSlashUrl(url).contains(cleanSlashUrl(u.getUrl())))
                .findFirst();
    }

    private IndexingResponse errorClassCheck(Exception e) {
        return e instanceof HttpStatusException && parseStatus(e.getMessage()) == 404 ?
                getIndRespError("Указанная страница не найдена или недоступна") :
                e instanceof IndexingServiceException ?
                        getIndRespError(e.getMessage()) :
                        getMessageForOtherExceptions();
    }

    private IndexingResponse getMessageForOtherExceptions() {
        return getIndRespError("Произошла ошибка индексации");
    }

    private void urlCheckHttp(String url) throws IndexingServiceException {
        if (url.indexOf("https://") != 0 && url.indexOf("http://") != 0) {
            throw new IndexingServiceException("Адресная строка должна начинаться с https:// или http://");
        }
    }

    private IndexingResponse indexPage(Site siteConf, String reqUrl)
            throws IOException {
        String siteUrl = siteConf.getUrl();
        Connection.Response response = cm.connection(reqUrl);
        List<SiteEntity> list = repJpaSite.findByUrl(siteUrl);
        return list.isEmpty() ?
                createSiteAndPage(response, siteConf, siteUrl) :
                deleteAndSaveNewPage(list, response, reqUrl);
    }

    private IndexingResponse createSiteAndPage(Connection.Response resp, Site siteConf, String reqUrl) {
        SiteEntity site = new SiteEntity(siteConf.getName(), siteConf.getUrl(), StatusType.INDEXING);
        repJpaSite.save(site);
        SiteWrap wrap = new SiteWrap(new TasksStopController(), allRepAndLemmaInstance, site);
        siteMapBean.getSiteMap().put(siteConf.getUrl(), wrap);
        wrap.createPage(reqUrl, resp);
        return getIndRespOk();
    }

    private IndexingResponse deleteAndSaveNewPage(List<SiteEntity> list, Connection.Response resp, String reqUrl) {
        SiteWrap wrap = siteMapBean.getSiteMap().get(list.get(0).getUrl());
        deletePageIfPresent(wrap, reqUrl);
        wrap.createPage(reqUrl, resp);
        return getIndRespOk();
    }

    private void deletePageIfPresent(SiteWrap wrap, String reqUrl) {
        int siteId = wrap.getSite().getId();
        List<PageEntity> pageList = repJpaPage.findByPathAndSiteId(parseUrlChild(reqUrl), siteId);
        if (!pageList.isEmpty()) {
            int pageId = pageList.get(0).getId();
            repJpaPage.deleteById(pageId);
            wrap.getUrlSet().remove(reqUrl);
        }
    }
}
