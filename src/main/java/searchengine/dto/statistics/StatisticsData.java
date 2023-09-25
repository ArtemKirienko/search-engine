package searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StatisticsData {
    private TotalStatistics total;
    private List<DetailedStatisticsItem> detailed;

    public StatisticsData(TotalStatistics total, List<DetailedStatisticsItem> detailed) {
        this.total = total;
        this.detailed = detailed;
    }
}
