package searchengine.config;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.helper.HttpConnection;
import org.springframework.stereotype.Component;
import searchengine.utils.Name;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Component
public class ConnectionMetod {
    private final ConnectionProperties cp;

    public Connection.Response connection(String path) throws IOException {
        return HttpConnection.connect(path)
                .timeout(10000)
                .userAgent(getElement(cp.getRefers()))
                .referrer(getElement(cp.getUsers()))
                .execute();
    }

    public String getElement(List<Name> values) {
        return Math.random() * 10 > 5 ? values.get(0).getValue() : values.get(1).getValue();
    }
}
