package searchengine.data.tasks;


import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import searchengine.data.TasksStopController;
import searchengine.data.SiteWrap;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import java.util.concurrent.RecursiveAction;

@Slf4j
@Setter
public class ParserRecursiveMain extends RecursiveAction {
    private SiteWrap wrap;
    private Connection.Response response;
    private volatile TasksStopController exemplStop;

    public ParserRecursiveMain(SiteWrap wrap, Connection.Response response) {
        this.wrap = wrap;
        this.exemplStop = wrap.getStopController();
        this.response = response;
    }

    protected void compute() {
        if (exemplStop.isStop()) {
            return;
        }
        try {
            wrap.createPage(wrap.getSite().getUrl(), response);
            wrap.taskFJP(response);
        }catch (Exception e){
            log.error(e.getClass().toString());
        }
        checkExampleStop();
    }

    private void checkExampleStop() {
        if (exemplStop.isStop()) {
            setFailedCancelled(wrap.getSite());
        } else {
            setIndexed(wrap.getSite());
        }
    }

    private void setIndexed(SiteEntity site) {
        site.setStatus(StatusType.INDEXED);
        wrap.getSiteRepository().save(site);
    }

    private void setFailedCancelled(SiteEntity site) {
        site.setStatus(StatusType.FAILED);
        site.setLastError("Индексация прервана пользователем");
        wrap.getSiteRepository().save(site);
    }
}
