package searchengine.dto.search;

import lombok.Data;

import java.util.List;
@Data
public class ResponseTrue {
    private boolean result = true;
    private int count;
    private List<SnippedObject> data;

    public ResponseTrue(List<SnippedObject> sObjL) {
        if(sObjL.isEmpty()){
            this.count = 0;
        }else {
            this.count = sObjL.size();
        }

        this.data = sObjL;
    }
}
