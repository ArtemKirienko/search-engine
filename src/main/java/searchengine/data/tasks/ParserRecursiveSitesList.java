package searchengine.data.tasks;


import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import searchengine.config.*;
import searchengine.data.Site;
import searchengine.data.TasksStopController;
import searchengine.data.SiteWrap;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveAction;

@Slf4j
@Getter
@Setter
public class ParserRecursiveSitesList extends RecursiveAction {
    private volatile TasksStopController exemplStop;
    private final AllRepAndLemmaInstance allRepAndLemmaInstance;
    private final SiteRepository siteRepository;
    private final SiteMapBean siteMapBean;
    private final SitesList sitesList;
    private final ExecuteIndicator executeIndicator;
    private final ConnectionMetod cm;


    public ParserRecursiveSitesList(TasksStopController exemplStop, AllRepAndLemmaInstance allRepAndLemmaInstance
            , SiteMapBean siteMapBean, SitesList sitesList, ExecuteIndicator executeIndicator) {
        this.exemplStop = exemplStop;
        this.allRepAndLemmaInstance = allRepAndLemmaInstance;
        siteRepository = allRepAndLemmaInstance.getRepJpaSite();
        this.siteMapBean = siteMapBean;
        this.sitesList = sitesList;
        this.executeIndicator = executeIndicator;
        cm = allRepAndLemmaInstance.getConnectionMetod();
    }

    @Override
    protected void compute() {
        LinkedList<ParserRecursiveMain> tasks = getTasksList();
        pushForkJoinTasks(tasks);
        executeIndicator.setExec(false);
    }

    private void pushForkJoinTasks(LinkedList<ParserRecursiveMain> tasks) {
        for (ParserRecursiveMain task : tasks) {
            task.fork();
        }
        for (ParserRecursiveMain task : tasks) {
            task.join();
        }
    }

    private LinkedList<ParserRecursiveMain> getTasksList() {
        LinkedList<ParserRecursiveMain> tasks = new LinkedList<>();
        Set<Site> sites = sitesList.getSites();
        for (Site s : sites) {
            try {
                Connection.Response resp = cm.connection(s.getUrl());
                if (resp.bodyAsBytes().length == 0) {
                    saveIndexingSiteError(s, "Главная страница сайта недоступна");
                    continue;
                }
                createAndAddListTask(s, resp, tasks);
            } catch (Exception e) {
                saveException(e, s);
            }
        }
        return tasks;
    }


    private void saveException(Exception e, Site s) {
        if (e instanceof IOException) {
            saveIndexingSiteError(s, "Нет соединения");
        } else {
            log.error(e.getClass().toString());
        }
    }

    private void createAndAddListTask(Site s, Connection.Response resp, LinkedList<ParserRecursiveMain> tasks) {
        if (exemplStop.isStop()) {
            saveIndexingSiteError(s, "Индексация прервана пользователем");
            return;
        }
        createAndAdd(s, resp, tasks);
    }

    private void createAndAdd(Site s, Connection.Response resp, LinkedList<ParserRecursiveMain> tasks) {
        deleteSite(s.getUrl());
        SiteEntity site = createSite(s);
        SiteWrap wrap = new SiteWrap(exemplStop, allRepAndLemmaInstance, site);
        siteMapBean.getSiteMap().put(s.getUrl(), wrap);
        ParserRecursiveMain parserTask = new ParserRecursiveMain(wrap, resp);
        tasks.add(parserTask);
    }

    private SiteEntity createSite(Site s) {
        SiteEntity site = new SiteEntity(s.getName(), s.getUrl(), StatusType.INDEXING);
        siteRepository.save(site);
        return site;
    }

    private void saveIndexingSiteError(Site s, String message) {
        List<SiteEntity> sites = siteRepository.findByUrl(s.getUrl());
        if (!sites.isEmpty()) {
            updateDataErrorSite(sites, message);
        } else {
            createSiteError(s, message);
        }
    }

    private void updateDataErrorSite(List<SiteEntity> sites, String message) {
        SiteEntity site = sites.get(0);
        site.setTimeNow();
        site.setLastError(message);
        siteRepository.save(site);
    }

    private void createSiteError(Site s, String message) {
        SiteEntity site = new SiteEntity(s.getName(), s.getUrl(), StatusType.FAILED, message);
        siteRepository.save(site);
    }

    private void deleteSite(String siteUrl) {
        customDeleteSiteByUrl(siteUrl);
        siteMapBean.getSiteMap().remove(siteUrl);
    }

    private void customDeleteSiteByUrl(String siteUrl) {
        List<SiteEntity> lsite = siteRepository.findByUrl(siteUrl);
        if (!lsite.isEmpty()) {
            siteRepository.deleteById(lsite.get(0).getId());
        }
    }


}
