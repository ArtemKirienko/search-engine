package searchengine.services;

import org.springframework.http.ResponseEntity;

import searchengine.dto.search.RequestObj;

import java.util.NoSuchElementException;

public interface SearchService {
    ResponseEntity search(RequestObj obj) throws NoSuchElementException;
}
