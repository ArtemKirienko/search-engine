package searchengine.config.pojo;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConfSite {
    private String url;
    private String name;

    @Override
    public int hashCode(){
        return url.hashCode();
    }
}
