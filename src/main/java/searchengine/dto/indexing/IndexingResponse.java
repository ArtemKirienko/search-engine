package searchengine.dto.indexing;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class IndexingResponse {
    private boolean result;
    private String error;

    public IndexingResponse(String error) {
        this.error = error;
    }

    public IndexingResponse() {
        this.result = true;
    }


}
