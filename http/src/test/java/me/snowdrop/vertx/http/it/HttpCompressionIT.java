package me.snowdrop.vertx.http.it;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import me.snowdrop.vertx.http.client.VertxClientHttpConnector;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.http.HttpHeaders.ACCEPT_ENCODING;

@Category(FastTests.class)
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
        "server.port=" + Ports.HTTP_COMPRESSION_IT,
        "server.compression.enabled=true"
    },
    classes = AbstractHttpIT.Routers.class
)
public class HttpCompressionIT extends AbstractHttpIT {

    private static final String BASE_URL = String.format("http://localhost:%d", Ports.HTTP_COMPRESSION_IT);

    @Autowired
    private Vertx vertx;

    @Override
    protected WebClient getClient() {
        HttpClientOptions options = new HttpClientOptions()
            .setTryUseCompression(true);

        return WebClient.builder()
            .defaultHeader(ACCEPT_ENCODING, "gzip")
            .clientConnector(new VertxClientHttpConnector(vertx, options))
            .baseUrl(BASE_URL)
            .build();
    }
}