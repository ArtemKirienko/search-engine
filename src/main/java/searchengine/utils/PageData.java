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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageData that = (PageData) o;
        return pageId == that.pageId && Float.compare(that.generalRank, generalRank) == 0;
    }

    @Override
    public int hashCode() {
        return pageId + Float.floatToIntBits(generalRank);
    }
}
