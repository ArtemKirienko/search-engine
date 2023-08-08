package searchengine.compAndPojoClass;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class Indexing {
    private volatile boolean indexing;
}
