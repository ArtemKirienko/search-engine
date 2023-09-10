package searchengine.dto.search;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private int count;
    private List<SnippedObject> data;
    private String error;

    public SearchResponse(List<SnippedObject> sObjL) {
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


}
