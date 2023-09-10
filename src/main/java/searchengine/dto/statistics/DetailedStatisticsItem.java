package searchengine.dto.statistics;

import lombok.Data;

@Data
public class DetailedStatisticsItem {
    private String url;
    private String name;
    private String status;
    private String statusTime;
    private String error;
    private int pages;
    private int lemmas;

    public DetailedStatisticsItem(String url, String name, String status, String statusTime, String error, int pages, int lemmas) {
        this.url = url;
        this.name = name;
        this.status = status;
        this.statusTime = statusTime;
        this.error = error;
        this.pages = pages;
        this.lemmas = lemmas;
    }


}
