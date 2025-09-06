package com.hortifruti.sl.hortifruti.service;

import com.hortifruti.sl.hortifruti.dto.DistanceResponse;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DistanceMatrixService {

  @Value("${google.maps.api.key}")
  private String apiKey;

  private static final String API_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";

  public DistanceResponse getDistance(
      double originLat, double originLng, double destLat, double destLng) {
    String url =
        UriComponentsBuilder.newInstance()
            .uri(java.net.URI.create(API_URL))
            .queryParam("origins", originLat + "," + originLng)
            .queryParam("destinations", destLat + "," + destLng)
            .queryParam("key", apiKey)
            .build()
            .toUriString();

    RestTemplate restTemplate = new RestTemplate();
    String response = restTemplate.getForObject(url, String.class);

    // Extraindo os dados do JSON
    // Aqui você pode usar uma biblioteca como o Jackson para mapear a resposta para
    // um objeto
    // ou realizar um parsing simples.

    // Aqui está a forma de acessar a distância e a duração da resposta:
    try {
      // Exemplo simplificado para extrair informações do JSON
      JSONObject jsonResponse = new JSONObject(response);
      JSONObject row = jsonResponse.getJSONArray("rows").getJSONObject(0);
      JSONObject element = row.getJSONArray("elements").getJSONObject(0);

      String distance = element.getJSONObject("distance").getString("text");
      String duration = element.getJSONObject("duration").getString("text");

      return new DistanceResponse(distance, duration);
    } catch (Exception e) {
      // Handle error if response parsing fails
      return new DistanceResponse("Erro", "Erro");
    }
  }
}
