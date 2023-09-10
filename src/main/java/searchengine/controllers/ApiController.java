package searchengine.controllers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import searchengine.dto.indexing.IndexingRequest;
import searchengine.dto.search.SearchRequest;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.*;

@Getter
@Setter
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping(value = "/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(IndexingRequest indexingRequest) {
        return ResponseEntity.ok(indexingService.addIndexPage(indexingRequest));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchQuery(SearchRequest searchRequest) {
        return ResponseEntity.ok(searchService.search(searchRequest));
    }
}

