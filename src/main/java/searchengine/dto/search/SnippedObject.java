package searchengine.dto.search;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Data
@RequiredArgsConstructor

public class SnippedObject implements Comparable<SnippedObject>{
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;

    public SnippedObject(String site, String siteName, String uri, String title, String snippet, float relevance) {
        this.site = site;
        this.siteName = siteName;
        this.uri = uri;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }



    @Override
    public int compareTo(SnippedObject o) {
        return Float.compare(o.getRelevance(), this.getRelevance());
    }




}
