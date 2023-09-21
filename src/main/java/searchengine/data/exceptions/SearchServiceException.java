package searchengine.data.exceptions;

public class SearchServiceException extends Exception {
    public SearchServiceException(String message){
        super(message);
    }

    public static String getTxtFirstLine(String txt){
        return  txt.split("\n")[0];

    }
}
