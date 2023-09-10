package searchengine.config.pojo.tasks;

import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import searchengine.config.pojo.TasksStopController;
import searchengine.config.pojo.SiteWrap;

import java.io.IOException;
import java.util.concurrent.ForkJoinTask;

import static searchengine.config.pojo.StaticMetods.connection;
import static searchengine.config.pojo.StaticMetods.parseStatus;

@Setter
public class ParserRecursive extends ForkJoinTask<Void> {
    private SiteWrap wrap;
    protected String path;
    protected volatile TasksStopController exemplStop;

    public ParserRecursive(String path, SiteWrap wrap) {
        synchronized (this) {
            this.wrap = wrap;
            this.path = path;
            this.exemplStop = wrap.getStopController();
        }
    }

    protected void compute() throws Exception {
        Connection.Response response = connection(path);
        wrap.createPage(path, response);
        wrap.taskFJP(response);
    }

    public boolean exec() {
        if (exemplStop.isStop()) {
            return true;
        }
        try {
            this.compute();
            return true;
        } catch (IOException e) {
            if (e instanceof HttpStatusException) {
                int statusCode = parseStatus(e.getMessage());
                wrap.createStatusErrorPage(path, statusCode);
                return true;
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public final Void getRawResult() {
        return null;
    }

    protected final void setRawResult(Void mustBeNull) {
    }
}
