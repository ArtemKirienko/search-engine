package searchengine.dto.startIndexing;

import lombok.Data;

@Data
public class IndexingResponse {
    private boolean result;
    private String error;

    public IndexingResponse(String error) {
        this.error = error;
    }
}
