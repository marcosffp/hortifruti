package com.hortifruti.sl.hortifruti.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Serviço para integração com Google Directions API.
 */
@Service
public class GoogleDirectionsService {

  @Value("${credentials_google}")
  private String apiKey;

  private final RestTemplate restTemplate = new RestTemplate();

  /**
   * Calcula rota, distância, tempo e valor do frete.
   */
  public RouteInfo routeAndFreight(String origin, String destination) {
    String url = UriComponentsBuilder
        .fromUriString("https://maps.googleapis.com/maps/api/directions/json")
        .queryParam("origin", origin)
        .queryParam("destination", destination)
        .queryParam("key", apiKey)
        .queryParam("language", "pt-BR")
        .toUriString();

    String response = restTemplate.getForObject(url, String.class);
    JSONObject json = new JSONObject(response);
    JSONArray routes = json.getJSONArray("routes");
    if (routes.isEmpty())
      throw new RuntimeException("Rota não encontrada");

    JSONObject leg = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0);

    int distanceMeters = leg.getJSONObject("distance").getInt("value");
    String distanceText = leg.getJSONObject("distance").getString("text");
    String durationText = leg.getJSONObject("duration").getString("text");
    String polyline = routes.getJSONObject(0).getJSONObject("overview_polyline").getString("points");

    double freight = calculateFreight(distanceMeters);

    return new RouteInfo(distanceText, durationText, freight, polyline);
  }

  /**
   * Lógica de cálculo do frete.
   * Exemplo: R$10,00 para até 200 metros (mesma rua), R$15,00 para demais
   * distâncias.
   */
  private double calculateFreight(int distanceMeters) {
    if (distanceMeters <= 200) {
      return 10.0;
    } else {
      return 15.0;
    }
  }

  // DTO para resposta da rota
  public static class RouteInfo {
    public String distance;
    public String duration;
    public double freight;
    public String polyline;

    public RouteInfo(String distance, String duration, double freight, String polyline) {
      this.distance = distance;
      this.duration = duration;
      this.freight = freight;
      this.polyline = polyline;
    }
  }
}