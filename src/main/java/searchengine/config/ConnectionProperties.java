package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import searchengine.data.Name;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "connection-settings")
public class ConnectionProperties {
  private  List<Name> refers;
  private  List<Name> users;
}
