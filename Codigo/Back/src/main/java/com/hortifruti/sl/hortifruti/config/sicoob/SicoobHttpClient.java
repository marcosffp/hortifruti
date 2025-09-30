package com.hortifruti.sl.hortifruti.config.sicoob;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SicoobHttpClient {

    @Value("${sicoob.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final SicoobToken sicoobToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode get(String endpoint) throws IOException {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        System.out.println("Fazendo requisição GET para: " + apiUrl + endpoint);
        ResponseEntity<String> response = restTemplate.exchange(
            apiUrl + endpoint, 
            HttpMethod.GET, 
            entity, 
            String.class
        );

        return objectMapper.readTree(response.getBody());
    }

    public JsonNode post(String endpoint, Object body) throws IOException {
        HttpHeaders headers = createHeaders();
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            apiUrl + endpoint, 
            entity, 
            String.class
        );

        return objectMapper.readTree(response.getBody());
    }

    public ResponseEntity<String> put(String endpoint, Object body) throws IOException {
        HttpHeaders headers = createHeaders();
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        
        return restTemplate.exchange(
            apiUrl + endpoint, 
            HttpMethod.PUT, 
            entity, 
            String.class
        );
    }

    public ResponseEntity<String> delete(String endpoint) throws IOException {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        return restTemplate.exchange(
            apiUrl + endpoint, 
            HttpMethod.DELETE, 
            entity, 
            String.class
        );
    }

    private HttpHeaders createHeaders() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(sicoobToken.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public String getApiUrl() {
        return this.apiUrl;
    }
}