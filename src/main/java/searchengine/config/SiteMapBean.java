package searchengine.config;

import lombok.Data;
import org.springframework.stereotype.Component;
import searchengine.config.pojo.SiteWrap;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
public class SiteMapBean {
    private Map<String, SiteWrap> siteMap = new HashMap<>();
}
