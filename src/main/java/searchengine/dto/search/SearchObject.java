package searchengine.dto.search;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchObject {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;

    public SearchObject(String site, String siteName, String uri, String title, String snipped) {
        this.site = site;
        this.siteName = siteName;
        this.uri = uri;
        this.title = title;
        this.snippet = snipped;
    }
}
