package com.bakeflow.inventory.infrastructure.external;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class OpenFoodFactsConfiguration {
    @Bean
    RestClient openFoodFactsRestClient(
            @Value("${open-food-facts.base-url}") String url,
            @Value("${open-food-facts.connect-timeout:2s}") Duration connectTimeout,
            @Value("${open-food-facts.read-timeout:4s}") Duration readTimeout) {
        var httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "BakeFlow/1.0 (inventory catalog)")
                .build();
    }
}
