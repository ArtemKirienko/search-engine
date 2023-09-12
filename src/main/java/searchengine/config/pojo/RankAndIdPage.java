package searchengine.config.pojo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RankAndIdPage implements Comparable<RankAndIdPage> {
   private Integer pageId;
   private Float generalRank;

    public RankAndIdPage(Integer pageId, Float generalRank) {
        this.pageId = pageId;
        this.generalRank = generalRank;
    }

    @Override
    public int compareTo(RankAndIdPage o) {
        if (this.getGeneralRank().equals(o.getGeneralRank())) {
            return 0;
        }
        return this.getGeneralRank() - o.getGeneralRank() > 0 ? -1 : 1;

    }
}
