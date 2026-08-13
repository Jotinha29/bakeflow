package com.bakeflow.integration.infrastructure;

import java.time.Duration;import org.springframework.beans.factory.annotation.*;import org.springframework.context.annotation.*;import org.springframework.http.client.SimpleClientHttpRequestFactory;import org.springframework.web.client.RestClient;

@Configuration
class ExternalClientsConfiguration {
 @Bean("openFoodFactsHttpClient") RestClient openFoodFacts(@Value("${open-food-facts.base-url}")String url,@Value("${open-food-facts.connect-timeout}")Duration connect,@Value("${open-food-facts.read-timeout}")Duration read){return client(url,connect,read);}
 @Bean("brasilApiHttpClient") RestClient brasilApi(@Value("${brasil-api.base-url}")String url,@Value("${brasil-api.connect-timeout}")Duration connect,@Value("${brasil-api.read-timeout}")Duration read){return client(url,connect,read);}
 private RestClient client(String url,Duration connect,Duration read){var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(connect);factory.setReadTimeout(read);return RestClient.builder().baseUrl(url).requestFactory(factory).build();}
}
