package searchengine.dto.statistics;

import lombok.Data;

@Data
public class TotalStatistics {
    private int sites;
    private int pages;
    private int lemmas;
    private boolean indexing;

    public TotalStatistics(int sites, boolean indexing) {
        this.sites = sites;
        this.indexing = indexing;
    }
}
