package searchengine.config.pojo.tasks;

import lombok.Setter;
import org.jsoup.Connection;
import searchengine.config.pojo.TasksStopController;
import searchengine.config.pojo.SiteWrap;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;

import java.util.concurrent.ForkJoinTask;

@Setter
public class ParserRecursiveMain extends ForkJoinTask<Void> {
    private SiteWrap wrap;
    private Connection.Response response;
    private volatile TasksStopController exemplStop;

    public ParserRecursiveMain(SiteWrap wrap, Connection.Response response) {
        this.wrap = wrap;
        this.exemplStop = wrap.getStopController();
        this.response = response;
    }

    private void compute() throws Exception {
        wrap.createPage(wrap.getSite().getUrl(), response);
        wrap.taskFJP(response);
    }

    public final Void getRawResult() {
        return null;
    }

    public final void setRawResult(Void mustBeNull) {
    }

    protected final boolean exec() {
        if (exemplStop.isStop()) {
            return true;
        }
        try {
            this.compute();
            if (exemplStop.isStop()) {
                setFailedCancelled(wrap.getSite());
                return true;
            } else {
                setIndexed(wrap.getSite());
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public void setIndexed(SiteEntity site) {
        site.setStatus(StatusType.INDEXED);
        wrap.getSiteRepository().save(site);
    }

    public void setFailedCancelled(SiteEntity site) {
        site.setStatus(StatusType.FAILED);
        site.setLastError("Индексация прервана пользователем");
        wrap.getSiteRepository().save(site);
    }
}
