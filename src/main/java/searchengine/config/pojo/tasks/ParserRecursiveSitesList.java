package searchengine.config.pojo.tasks;


import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import searchengine.config.AllRepAndLemmaInstance;
import searchengine.config.ExecuteIndicator;
import searchengine.config.SiteMapBean;
import searchengine.config.SitesList;
import searchengine.config.pojo.ConfSite;
import searchengine.config.pojo.TasksStopController;
import searchengine.config.pojo.SiteWrap;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveAction;

import static searchengine.config.pojo.StaticMetods.*;

@Getter
@Setter
public class ParserRecursiveSitesList extends RecursiveAction {
    private LinkedList<ParserRecursiveMain> list = new LinkedList<>();
    private volatile TasksStopController exemplStop;
    private final AllRepAndLemmaInstance allRepAndLemmaInstance;
    private final SiteRepository siteRepository;
    private final SiteMapBean siteMapBean;
    private final SitesList sitesList;
    private final ExecuteIndicator executeIndicator;

    public ParserRecursiveSitesList(TasksStopController exemplStop, AllRepAndLemmaInstance allRepAndLemmaInstance
            , SiteMapBean siteMapBean, SitesList sitesList, ExecuteIndicator executeIndicator) {
        this.exemplStop = exemplStop;
        this.allRepAndLemmaInstance = allRepAndLemmaInstance;
        siteRepository = allRepAndLemmaInstance.getRepJpaSite();
        this.siteMapBean = siteMapBean;
        this.sitesList = sitesList;
        this.executeIndicator = executeIndicator;
    }

    @Override
    protected void compute() {
        Set<ConfSite> sites = sitesList.getSites();
        for (ConfSite s : sites) {
            try {
                Connection.Response resp = connection(s.getUrl());
                if (resp.bodyAsBytes().length == 0) {
                    saveExceptionTxt(s, "Главная страница сайта недоступна");
                    continue;
                }
                creatAndPushTask(s, resp);
            } catch (Exception e) {
                if (e instanceof IOException) {
                    saveExceptionTxt(s, "Нет соединения");
                }
            }
        }
        for (ParserRecursiveMain task : list) {
            task.fork();
        }
        for (ParserRecursiveMain task : list) {
            task.join();
        }
        executeIndicator.setExec(false);
    }


    public void creatAndPushTask(ConfSite s, Connection.Response resp) {
        if (exemplStop.isStop()) {
            saveStopSiteInexing(s);
            return;
        }
        deleteSite(s.getUrl());
        SiteEntity site = new SiteEntity(s.getName(), s.getUrl(), StatusType.INDEXING);
        siteRepository.save(site);
        SiteWrap wrap = new SiteWrap(exemplStop, allRepAndLemmaInstance, site);
        siteMapBean.getSiteMap().put(s.getUrl(), wrap);
        ParserRecursiveMain parserTask = new ParserRecursiveMain(wrap, resp);
        list.add(parserTask);
    }

    public void saveStopSiteInexing(ConfSite s) {
        SiteEntity site =
                new SiteEntity(s.getName(), s.getUrl(), StatusType.FAILED, "Индексация прервана пользователем");
        siteRepository.save(site);
    }

    public void saveExceptionTxt(ConfSite s, String exceptTxt) {
        List<SiteEntity> lsite = siteRepository.findByUrl(s.getUrl());
        if (!lsite.isEmpty()) {
            SiteEntity site = lsite.get(0);
            site.setTimeNow();
            site.setLastError(exceptTxt);
            siteRepository.save(site);
        } else {
            SiteEntity site = new SiteEntity(s.getName(), s.getUrl(), StatusType.FAILED, exceptTxt);
            siteRepository.save(site);
        }
    }

    public void customDeleteSiteByUrl(String siteUrl) {
        List<SiteEntity> lsite = siteRepository.findByUrl(siteUrl);
        if (!lsite.isEmpty()) {
            siteRepository.deleteById(lsite.get(0).getId());
        }
    }

    public void deleteSite(String siteUrl) {
        customDeleteSiteByUrl(siteUrl);
        siteMapBean.getSiteMap().remove(siteUrl);
    }
}
