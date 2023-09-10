package searchengine.config;

import lombok.Data;
import org.springframework.stereotype.Component;
import searchengine.config.pojo.TasksStopController;

@Data
@Component
public class TaskStopControllerBean {
    private volatile TasksStopController tasksStopController;
}
