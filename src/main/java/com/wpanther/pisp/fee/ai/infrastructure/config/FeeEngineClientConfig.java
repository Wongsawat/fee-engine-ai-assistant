package com.wpanther.pisp.fee.ai.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class FeeEngineClientConfig {

    @Bean
    public RestClient feeEngineRestClient(
            @Value("${fee-engine.base-url}") String baseUrl,
            @Value("${fee-engine.timeout-seconds:10}") int timeoutSeconds) {
        Duration t = Duration.ofSeconds(timeoutSeconds);
        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.detect()
                .build(ClientHttpRequestFactorySettings.defaults()
                        .withConnectTimeout(t)
                        .withReadTimeout(t));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
