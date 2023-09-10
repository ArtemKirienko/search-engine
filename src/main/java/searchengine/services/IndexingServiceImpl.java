package searchengine.services;

import lombok.Data;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.springframework.stereotype.Service;
import searchengine.config.ExecuteIndicator;
import searchengine.config.pojo.*;
import searchengine.config.*;
import searchengine.config.pojo.tasks.ParserRecursiveSitesList;
import searchengine.dto.indexing.IndexingRequest;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
import searchengine.repository.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static searchengine.config.pojo.StaticMetods.*;


@Data
@Service
public class IndexingServiceImpl implements IndexingService {
    private final static ForkJoinPool example = new ForkJoinPool();
    private final TaskStopControllerBean stopControllerBean;
    private final AllRepAndLemmaInstance allRepALemm;
    private final SitesList sites;
    private final ExecuteIndicator indicator;
    private final SiteRepository repJpaSite;
    private final PageRepository repJpaPage;
    private final SiteMapBean siteMapBean;

    public synchronized void putSiteMap(String siteUrl, SiteWrap wrap) {
        siteMapBean.getSiteMap().put(siteUrl, wrap);
    }

    @Override
    public IndexingResponse startIndexing() {
        if (indicator.isExec()) {
            return getIndRespError("Индексация уже запущена");
        }
        indicator.setExec(true);
        TasksStopController stopCont = new TasksStopController();
        stopControllerBean.setTasksStopController(stopCont);
        ParserRecursiveSitesList task =
                new ParserRecursiveSitesList(stopCont, allRepALemm, siteMapBean, sites, indicator);
        example.invoke(task);
        return getIndRespOk();
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!indicator.isExec()) {
            return getIndRespError("Индексация не запущена");
        }
        stopControllerBean.getTasksStopController().setStop(true);
        indicator.setExec(false);
        return getIndRespOk();
    }

    @Override
    public IndexingResponse addIndexPage(IndexingRequest indexingRequest) {
        String value = urlCheck(indexingRequest.getUrl());
        if (!value.equals("")) {
            return getIndRespError(value);
        }
        String url = indexingRequest.getUrl();
        Optional<ConfSite> optSiteConf =
                sites.getSites().stream().filter(u -> url.contains(cleanSlashUrl(u.getUrl()))).findFirst();
        try {
            if (optSiteConf.isPresent()) {
                return indexPage(optSiteConf.get());
            }
        } catch (Exception ex) {
            if (ex instanceof HttpStatusException && parseStatus(ex.getMessage()) == 404) {
                return getIndRespError("Указанная страница не найдена");
            }
            return getIndRespError("Произошла ошибка индексации");
        }
        return getIndRespError("Сайт находиться за пределами индексируемого списка сайтов");
    }

    public String urlCheck(String url) {
        if (!(url.indexOf("https://") == 0 || url.indexOf("http://") == 0)) {
            return "Адресная строка должна начинаться с https:// или http://";
        }
        return "";
    }

    public IndexingResponse indexPage(ConfSite siteConf)
            throws IOException {
        String siteUrl = siteConf.getUrl();
        Connection.Response response = connection(siteUrl);
        List<SiteEntity> list = repJpaSite.findByUrl(siteConf.getUrl());
        if (list.isEmpty()) {
            return createSiteAndPage(response, siteConf, siteUrl);
        }
        SiteWrap wrap = siteMapBean.getSiteMap().get(list.get(0).getUrl());
        deletePageIfPresent(wrap, siteUrl);
        wrap.createPage(siteUrl, response);
        return getIndRespOk();
    }

    public IndexingResponse createSiteAndPage(Connection.Response response, ConfSite siteConf, String siteUrl) {
        SiteEntity site = new SiteEntity(siteConf.getName(), siteConf.getUrl(), StatusType.INDEXING);
        repJpaSite.save(site);
        SiteWrap wrap = new SiteWrap(new TasksStopController(), allRepALemm, site);
        putSiteMap(siteConf.getUrl(), wrap);
        wrap.createPage(siteUrl, response);
        return getIndRespOk();
    }

    public void deletePageIfPresent(SiteWrap wrap, String str) {
        Set<String> urlSet = wrap.getUrlSet();
        if (urlSet.contains(str)) {
            int siteId = wrap.getSite().getId();
            List<PageEntity> pageList = repJpaPage.findByPathAndSiteId(str, siteId);
            if (!pageList.isEmpty()) {
                repJpaPage.deleteById(pageList.get(0).getId());
            }
            urlSet.remove(str);
        }
    }
}
