package searchengine.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import searchengine.data.TasksStopController;

@Getter
@Setter
@RequiredArgsConstructor
@Component
public class TaskStopControllerBean {
    private volatile TasksStopController tasksStopController;
}
