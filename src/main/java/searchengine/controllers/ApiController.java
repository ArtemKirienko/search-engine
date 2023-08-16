package searchengine.controllers;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.IndExec;


import searchengine.dto.ResponseTrue;
import searchengine.dto.search.RequestObj;
import searchengine.dto.startIndexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.*;

import java.util.NoSuchElementException;

@Getter
@Setter
@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final StartIndexing startIndexing;
    private final IndExec indexing;
    private final SearchService searchService;
    private final AddPage addPage;

    public ApiController(StatisticsService statisticsService, StartIndexing startIndexing, IndExec indexing, SearchService searchService, AddPage addPage) {
        this.statisticsService = statisticsService;
        this.startIndexing = startIndexing;
        this.indexing = indexing;
        this.searchService = searchService;
        this.addPage = addPage;
    }


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        if (indexing.isExec()) {
            return new ResponseEntity(new IndexingResponse("Индексация уже запущена"), HttpStatus.BAD_REQUEST);
        } else {
            startIndexing.startIndexing();
            return new ResponseEntity(new ResponseTrue(), HttpStatus.OK);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        if (indexing.isExec()) {
            startIndexing.stopIndexing();
            return new ResponseEntity(new ResponseTrue(), HttpStatus.OK);
        } else {
            return new ResponseEntity(new IndexingResponse("Индексация не запущена"), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity aDDindexPage(@RequestBody String string) {
        return addPage.addIndexPage(string);
    }

    @GetMapping("/search")
    public ResponseEntity searchQuery(RequestObj obj) {
        try {
            return searchService.search(obj);
        } catch (NoSuchElementException e) {
            return new ResponseEntity(new IndexingResponse("Передана пустая строка"), HttpStatus.BAD_REQUEST);
        }
    }
}

