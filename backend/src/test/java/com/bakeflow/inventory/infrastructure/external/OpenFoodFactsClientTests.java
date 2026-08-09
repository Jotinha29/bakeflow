package com.bakeflow.inventory.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakeflow.inventory.application.InventoryDtos.LookupStatus;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class OpenFoodFactsClientTests {
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        server.start();
    }

    @AfterEach void stop() { server.stop(0); }

    @Test
    void mapsFoundProductToInternalContract() {
        String json = "{\"status\":1,\"product\":{\"product_name\":\"Chocolate\",\"brands\":\"Demo Brand\",\"quantity\":\"100 g\",\"image_front_url\":\"https://example.test/image.jpg\",\"categories_tags\":[\"en:chocolates\"]}}";
        json("/api/v2/product/789.json", 200, json, 0);
        var result = client(Duration.ofSeconds(1)).findByBarcode("789");
        assertThat(result.status()).isEqualTo(LookupStatus.FOUND);
        assertThat(result.name()).isEqualTo("Chocolate");
        assertThat(result.categories()).containsExactly("chocolates");
    }

    @Test void mapsMissingProduct() {
        json("/api/v2/product/404.json", 200, "{\"status\":0}", 0);
        assertThat(client(Duration.ofSeconds(1)).findByBarcode("404").status()).isEqualTo(LookupStatus.NOT_FOUND);
    }

    @Test void convertsApiFailureToUnavailable() {
        json("/api/v2/product/500.json", 500, "{}", 0);
        assertThat(client(Duration.ofSeconds(1)).findByBarcode("500").status()).isEqualTo(LookupStatus.UNAVAILABLE);
    }

    @Test void convertsTimeoutToUnavailable() {
        json("/api/v2/product/slow.json", 200, "{\"status\":1,\"product\":{\"product_name\":\"Late\"}}", 250);
        assertThat(client(Duration.ofMillis(30)).findByBarcode("slow").status()).isEqualTo(LookupStatus.UNAVAILABLE);
    }

    private OpenFoodFactsClient client(Duration timeout) {
        var factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(timeout);
        return new OpenFoodFactsClient(RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build());
    }

    private void json(String path, int status, String body, long delay) {
        server.createContext(path, exchange -> {
            try {
                if (delay > 0) Thread.sleep(delay);
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
    }
}
