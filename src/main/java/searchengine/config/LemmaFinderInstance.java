package searchengine.config;

import lombok.Getter;
import org.springframework.stereotype.Component;
import searchengine.config.pojo.LemmaFinder;

import java.io.IOException;

@Component
@Getter
public class LemmaFinderInstance {
    private LemmaFinder lemmaFinder = LemmaFinder.getInstance();

    public LemmaFinderInstance() throws IOException {
    }
}
