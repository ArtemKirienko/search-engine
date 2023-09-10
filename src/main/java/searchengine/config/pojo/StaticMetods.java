package searchengine.config.pojo;

import org.jsoup.Connection;
import org.jsoup.helper.HttpConnection;
import org.springframework.stereotype.Component;
import searchengine.config.ConnectionProperties;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SnippedObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class StaticMetods {
    private static final List<Name> referList = new ArrayList<>();
    private static final List<Name> userList = new ArrayList<>();

    public StaticMetods(ConnectionProperties cp) {
        userList.addAll(cp.getUsers());
        referList.addAll(cp.getRefers());
    }

    public static String getElement(List<Name> values) {
        String value = Math.random() * 10 > 5 ? values.get(0).getValue() : values.get(1).getValue();
        return value;
    }

    public static Connection.Response connection(String path) throws IOException {
        return HttpConnection.connect(path)
                .timeout(10000)
                .userAgent(getElement(referList))
                .referrer(getElement(userList))
                .execute();
    }

    public static Integer parseStatus(String txt) {
        int v = txt.indexOf("us=");
        String tx = txt.substring(v + 3, v + 6);
        return Integer.parseInt(tx);
    }

    public static boolean getFiltr(String attrHref) {
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

    public static String cleanWwwUrl(String url) {
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

    public static boolean urlsFiltr(String domUrl, String attrHref) {
        return (attrHref.contains(domUrl))
                || attrHref.contains(cleanWwwUrl(domUrl))
                && getFiltr(attrHref)
                && attrHref.indexOf("http") == 0;
    }

    public static synchronized IndexingResponse getIndRespOk() {
        return new IndexingResponse();
    }

    public static synchronized IndexingResponse getIndRespError(String str) {
        return new IndexingResponse(str);
    }

    public static synchronized SearchResponse getSearchRespOk(List<SnippedObject> snippedObject) {
        return new SearchResponse(snippedObject);
    }

    public static synchronized SearchResponse getSearchresponsError(String message) {
        return new SearchResponse(message);
    }

}
