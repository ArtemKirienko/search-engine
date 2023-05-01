package searchengine.config;

import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


@Component
public class ControllerStartStop {

    private volatile boolean indexingStart = false;


    public Boolean getIndStart() {return indexingStart;}
    public void setIndStart(boolean value) {indexingStart = value;}


}
