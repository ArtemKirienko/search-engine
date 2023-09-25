package searchengine.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageData implements Comparable<PageData> {
   private Integer pageId;
   private float generalRank;

    public PageData(Integer pageId, Float generalRank) {
        this.pageId = pageId;
        this.generalRank = generalRank;
    }

    @Override
    public int compareTo(PageData o) {
        if (this.getGeneralRank() == (o.getGeneralRank())) {
            return 0;
        }
        return this.getGeneralRank() - o.getGeneralRank() > 0 ? -1 : 1;

    }
}
