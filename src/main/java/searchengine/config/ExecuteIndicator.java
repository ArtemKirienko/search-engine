package searchengine.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class ExecuteIndicator {
     private volatile boolean exec;
}
