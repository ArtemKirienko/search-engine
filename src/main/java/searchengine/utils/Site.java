package searchengine.utils;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Site {
    private String url;
    private String name;

    @Override
    public int hashCode(){
        return url.hashCode();
    }
}
