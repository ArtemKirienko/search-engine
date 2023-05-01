package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.config.ControllerStartStop;


public interface StartIndexing   {
    void startIndexing() ;
    void stopIndexing() ;
    boolean addIndexPage(String string) ;
}
