package tech.sledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.net.http.HttpClient;

public class HttpClientTest extends BaseTest {
    @Autowired
    HttpClient httpClient;

    @Test
    void httpClient() {
        assert httpClient != null;
    }
}
