package searchengine.dto.startIndexing;

import lombok.Getter;

@Getter
public class ResponseFalse {

    private String result = "false";
    private String error ;
public ResponseFalse(String error){
     this.error = error;
}
}
