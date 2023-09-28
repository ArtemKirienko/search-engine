package searchengine.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageData implements Comparable<PageData> {
    private int pageId;
    private float generalRank;

    public PageData(int pageId, float generalRank) {
        this.pageId = pageId;
        this.generalRank = generalRank;
    }

    @Override
    public int compareTo(PageData o) {
        if (Float.compare(generalRank, o.generalRank) == 0) {
            return 0;
        }
        return this.getGeneralRank() - o.getGeneralRank() > 0 ? -1 : 1;
    }
}
