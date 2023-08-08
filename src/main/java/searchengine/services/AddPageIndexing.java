package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.compAndPojoClass.LemmaFinder;
import searchengine.compAndPojoClass.SiteConf;
import searchengine.config.SitesList;
import searchengine.dto.ResponseTrue;
import searchengine.dto.startIndexing.IndexingResponse;
import searchengine.exeptionClass.ExceedingNumberPages;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.StatusType;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static searchengine.services.StartIndexingImpl.*;

@RequiredArgsConstructor
@Service
public class AddPageIndexing implements AddPage {
    private final SitesList sites;
    @Autowired
    private StartIndexingImpl startIndexing;

    @Override
    public ResponseEntity addIndexPage(String str) {
        if (str.contains("url=http://")) {
            return responseIndexPageError("Адресная строка должна начинаться с http:// или https://");
        }
        String string = decodeURL(str);
        System.out.println(string);
        if (!string.contains("http://") && !string.contains("https://")) {
            return responseIndexPageError("Адресная строка должна содержать http:// или https://");
        }
        if (string.indexOf("http://") != 0 && string.indexOf("https://") != 0) {
            return responseIndexPageError("Адресная строка должна начинаться с http:// или https://");
        }
        Optional<SiteConf> siteDom =
                sites.getSites().stream()
                        .filter(u -> string.contains(cleanSlashUrl(u.getUrl()))).findFirst();
        try {
            if (siteDom.isPresent()) {
                return addIndexPageIfPresentSiteConf(siteDom.get(), string);
            }
        } catch (IOException ex) {
            if (ex instanceof HttpStatusException) {
                if (parseStatus(ex.getMessage()) == 404) {
                    return new ResponseEntity(
                            new IndexingResponse("Указанная страница не найдена"), HttpStatus.NOT_FOUND);
                }
            }
        } catch (Exception ex) {
            new ResponseEntity(
                    new IndexingResponse("Указанная страница не найдена"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return responseIndexPageError("");
    }

    public ResponseEntity responseIndexPageError(String error) {
        return new ResponseEntity(new IndexingResponse(error), HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity addIndexPageIfPresentSiteConf(SiteConf siteConf, String string)
            throws IOException, ExceedingNumberPages {
        Connection.Response response = connection(string);
        List<Site> list = startIndexing.getRepJpaSite().findByUrl(siteConf.getUrl());
        if (list.isEmpty()) {
            Site site = new Site(siteConf.getName(), siteConf.getUrl(), StatusType.INDEXING);
            startIndexing.getRepJpaSite().save(site);
            StartIndexingImpl.StopControllerWokerTask defStopCont = startIndexing.new StopControllerWokerTask();
            StartIndexingImpl.StopControllerWokerTask.Wrap wrap = defStopCont.new Wrap(site);
            wrap.setLemmaFinder(LemmaFinder.getInstance());
            wrap.andUpdatePage(wrap.createPage(string, site), site, response);
            synchronized (startIndexing.pageMap) {
                startIndexing.pageMap.put(siteConf.getUrl(), wrap);
            }
            return new ResponseEntity(new ResponseTrue(), HttpStatus.OK);
        }
        StartIndexingImpl.StopControllerWokerTask.Wrap wrap = startIndexing.pageMap.get(list.get(0).getUrl());
        synchronized (wrap.pageSett) {
            if (wrap.pageSett.contains(string)) {
                Page page = wrap.getSite().getPageSet().stream().filter(p -> p.getPath().equals(
                        parseUrlChild(cleanSlashUrl(string)))).findFirst().get();
                startIndexing.getRepJpaPage().deleteById(page.getId());
                wrap.pageSett.remove(string);
                wrap.andUpdatePage(wrap.createPage(string, list.get(0)), list.get(0), response);
                return new ResponseEntity(new ResponseTrue(), HttpStatus.OK);
            }
            wrap.andUpdatePage(wrap.createPage(string, list.get(0)), list.get(0), response);
            return new ResponseEntity(new ResponseTrue(), HttpStatus.OK);
        }
    }

    public String decodeURL(String string) {
        int in = string.indexOf("http");
        StringBuilder sb = new StringBuilder();
        int count = 3;
        int first = 0;
        int second = 0;
        byte res;
        for (; in < string.length(); in++) {
            char el = string.charAt(in);
            if (count == 2) {
                first = Character.digit(string.charAt(in), 16);
                --count;
                continue;
            }
            if (count == 1) {
                second = Character.digit(string.charAt(in), 16);
                --count;
                continue;
            }
            if (count == 0) {
                res = (byte) ((first << 4) + second);
                count = 3;
                char s = (char) res;
                sb.append(s);
            }
            if (el == '%') {
                --count;
                continue;
            }
            sb.append(el);
        }
        return sb.toString();
    }
}
