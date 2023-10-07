package searchengine.utils;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Setter
@Getter
public class Site {
    private String url;
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Site site = (Site) o;
        return Objects.equals(url, site.url) && Objects.equals(name, site.name);
    }

    @Override
    public int hashCode(){
        return url.hashCode();
    }
}
