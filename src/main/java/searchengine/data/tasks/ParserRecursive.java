package searchengine.data.tasks;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import searchengine.config.ConnectionMetod;
import searchengine.data.TasksStopController;
import searchengine.data.SiteWrap;

import java.util.concurrent.RecursiveAction;

import static searchengine.data.UrlUtils.parseStatus;
@Slf4j
@Setter
public class ParserRecursive extends RecursiveAction {
    private final ConnectionMetod cm;
    private SiteWrap wrap;
    protected String path;
    protected volatile TasksStopController exemplStop;

    public ParserRecursive(String path, SiteWrap wrap, ConnectionMetod cm) {
        synchronized (this) {
            this.cm = cm;
            this.wrap = wrap;
            this.path = path;
            this.exemplStop = wrap.getStopController();
        }
    }

    protected void compute() {
        if (exemplStop.isStop()) {
            return;
        }
        try {
            Connection.Response response = cm.connection(path);
            wrap.createPage(path, response);
            wrap.taskFJP(response);
        } catch (Exception e) {
            if (e instanceof HttpStatusException) {
                int statusCode = parseStatus(e.getMessage());
                wrap.createStatusErrorPage(path, statusCode);
            }
            log.error(e.getClass().toString());
        }
    }

}
