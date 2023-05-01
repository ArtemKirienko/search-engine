package searchengine.dto.search;

import lombok.Data;

@Data
public class RequestObj {
    private String query;
   private int offset;
   private int limit;
   private String siteUrl;

    public RequestObj(String query, int offset, int limit, String siteUrl) {
        this.query = query;
        this.offset = offset;
        this.limit = limit;
        this.siteUrl = siteUrl;
    }
}
