package searchengine.compAndPojoClass;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SiteConf implements Comparable<SiteConf>{
    private String url;
    private String name;

    @Override
    public int compareTo(SiteConf sc) {
        return url.compareTo(sc.getUrl());
    }
}
