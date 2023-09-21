package searchengine.dto.search;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchRequest {
    private String query;
    private int offset;
    private int limit;
    private String siteUrl;
}
