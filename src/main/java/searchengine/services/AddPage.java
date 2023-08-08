package searchengine.services;

import org.springframework.http.ResponseEntity;

public interface AddPage {
    ResponseEntity addIndexPage(String str);
}
