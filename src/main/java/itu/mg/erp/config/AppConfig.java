package itu.mg.erp.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody()));
                    String body = reader.lines().collect(Collectors.joining());
                    throw new HttpClientErrorException(response.getStatusCode(), response.getStatusText(), body.getBytes(), null);
                }
            }
        });
        return restTemplate;
    }
}
