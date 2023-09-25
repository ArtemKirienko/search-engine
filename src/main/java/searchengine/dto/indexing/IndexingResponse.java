package searchengine.dto.indexing;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndexingResponse {
    private boolean result;
    private String error;

    public IndexingResponse(String error) {
        this.error = error;
    }

    public IndexingResponse() {
        this.result = true;
    }

    public static synchronized IndexingResponse getIndRespOk() {
        return new IndexingResponse();
    }

    public static synchronized IndexingResponse getIndRespError(String str) {
        return new IndexingResponse(str);
    }

}
