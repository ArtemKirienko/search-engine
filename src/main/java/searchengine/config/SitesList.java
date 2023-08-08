package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import searchengine.compAndPojoClass.SiteConf;

import java.util.TreeSet;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesList {
    private TreeSet<SiteConf> sites;
}
