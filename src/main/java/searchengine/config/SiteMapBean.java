package searchengine.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.data.SiteWrap;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
@Component
public class SiteMapBean {
    private final Map<String, SiteWrap> siteMap = new HashMap<>();

}
