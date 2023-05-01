package searchengine.controllers;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.ControllerStartStop;


import searchengine.dto.search.RequestObj;
import searchengine.dto.startIndexing.ResponseFalse;
import searchengine.dto.startIndexing.ResponseTrue;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repository.Repo;
import searchengine.services.SearchService;
import searchengine.services.StartIndexing;
import searchengine.services.StatisticsService;

import java.util.NoSuchElementException;

@Getter
@Setter
@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final StartIndexing startIndexing;
    private final ControllerStartStop exemplContr;
    private final SearchService searchService;



    public ApiController(StatisticsService statisticsService, StartIndexing startIndexing, ControllerStartStop exemplContr,SearchService searchService,Repo rep) {
        this.statisticsService = statisticsService;
        this.startIndexing = startIndexing;
        this.exemplContr = exemplContr;
        this.searchService = searchService;

    }

    @GetMapping("/statistics")
    ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    ResponseEntity startIndexing() {

        if (exemplContr.getIndStart()) {

            return new ResponseEntity(new ResponseFalse("Индексация уже запущена"), HttpStatus.BAD_REQUEST);
        } else {

            startIndexing.startIndexing();

            return new ResponseEntity(new ResponseTrue(), HttpStatus.OK);

        }

    }

    @GetMapping("/stopIndexing")
    ResponseEntity stopIndexing() {

        if (exemplContr.getIndStart()) {

            startIndexing.stopIndexing();

            return new ResponseEntity(new ResponseTrue(), HttpStatus.OK);

        } else {
            return new ResponseEntity(new ResponseFalse("Индексация не запущена"), HttpStatus.BAD_REQUEST);
        }

    }
    @PostMapping("/indexPage")
      ResponseEntity aDDindexPage(@RequestBody String string){

       if (startIndexing.addIndexPage(string)){
           return new ResponseEntity<>(new ResponseTrue(),HttpStatus.OK);
       }
       return new ResponseEntity(new ResponseFalse("Данная страница находится за пределами сайтов," +
               "указанных в конфигурационном файле"),HttpStatus.BAD_REQUEST);

    }

    @GetMapping("/search")

     ResponseEntity

    searchQuery(RequestObj obj) {
        try {

            return   searchService.search( obj);
        }catch (NoSuchElementException e){

            return new ResponseEntity(new ResponseFalse("Передана пустая строка"),HttpStatus.BAD_REQUEST );
        }



   }
}

