package searchengine.services;

import searchengine.dto.search.SearchRequest;
import searchengine.dto.search.SearchResponse;

import java.util.NoSuchElementException;

public interface SearchService {
    SearchResponse search(SearchRequest obj);
}
