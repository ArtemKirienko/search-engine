package searchengine.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UrlUtils {

    public static Integer parseStatus(String txt) {
        int v = txt.indexOf("us=");
        String tx = txt.substring(v + 3, v + 6);
        return Integer.parseInt(tx);
    }

    public static boolean getFiltrByEndsWith(String attrHref) {
        return !attrHref.endsWith(".pdf")
                && !attrHref.endsWith(".php")
                && !attrHref.endsWith(".jpg")
                && !attrHref.endsWith(".png");
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

    public static boolean urlsFiltrByDomainPresent(String domUrl, String attrHref) {
        return (attrHref.contains(domUrl)) || attrHref.contains(cleanWwwUrl(domUrl))
                && getFiltrByEndsWith(attrHref) && attrHref.indexOf("http://") == 0;
    }

    public static boolean urlFilterByNotDomain(String attrHref){
       return attrHref.charAt(0) == '/' && getFiltrByEndsWith(attrHref);
    }
}
