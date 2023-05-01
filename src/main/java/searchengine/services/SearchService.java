package searchengine.services;

import org.springframework.http.ResponseEntity;

import searchengine.dto.search.RequestObj;

import java.util.NoSuchElementException;

public interface SearchService {


     ResponseEntity//<ResponseTrue>
    // search (String query, int offset, int limit, String site) throws NoSuchElementException;
     search (RequestObj obj) throws NoSuchElementException;
}
