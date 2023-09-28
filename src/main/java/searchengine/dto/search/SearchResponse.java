package searchengine.dto.search;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class SearchResponse {
    private boolean result;
    private int count;
    private List<SearchObject> data;
    private String error;

    public SearchResponse(List<SearchObject> sObjL) {
        this.result = true;
        if (sObjL.isEmpty()) {
            this.count = 0;
        } else {
            this.count = sObjL.size();
        }
        this.data = sObjL;
    }

    public SearchResponse(String error) {
        this.error = error;
    }

    public static synchronized SearchResponse getSearchRespOk(List<SearchObject> objects) {
        return new SearchResponse(objects);
    }

    public static synchronized SearchResponse getSearchRespError(String message) {
        return new SearchResponse(message);
    }
}
