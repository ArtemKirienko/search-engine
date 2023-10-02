package searchengine.utils;

import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;

@Getter
@Setter
public class PageData {
    private int pageId;
    private float generalRank;

    public PageData(int pageId, float generalRank) {
        this.pageId = pageId;
        this.generalRank = generalRank;
    }

   public static Comparator<PageData> generalRankComparator = (o1, o2) -> {
       if (Float.compare(o1.getGeneralRank(), o2.getGeneralRank()) == 0) {
           return 0;
       }
       return o1.getGeneralRank() - o2.getGeneralRank() < 0 ? 1 : -1;
   };
}
