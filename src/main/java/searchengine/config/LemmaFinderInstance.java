package searchengine.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.utils.LemmaFinder;

import java.io.IOException;
@Slf4j
@Getter
@Component
public class LemmaFinderInstance {
    private final LemmaFinder lemmaFinder = LemmaFinder.getInstance();

    public LemmaFinderInstance() throws IOException {
        log.error("Creating LemmaFinderInstanse Exception");
    }
}
