package com.hortifruti.sl.hortifruti.service.freight;

import com.hortifruti.sl.hortifruti.dto.freight.DistanceFreightResponse;
import com.hortifruti.sl.hortifruti.dto.freight.DistanceResponse;
import com.hortifruti.sl.hortifruti.dto.freight.FreightCalculationRequest;
import com.hortifruti.sl.hortifruti.dto.freight.LocationRequest;
import com.hortifruti.sl.hortifruti.exception.DistanceException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class DistanceMatrixService {

  @Value("${google.maps.api.key}")
  private String apiKey;

  private static final String API_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";

  private final FreightService freightService;

  public DistanceFreightResponse calculateDistanceAndFreight(LocationRequest locationRequest) {
    DistanceResponse distanceResponse = fetchDistanceAndDuration(locationRequest);

    FreightCalculationRequest freightRequest = createFreightRequest(distanceResponse);
    double freight = freightService.calculateFreight(freightRequest);

    return buildDistanceFreightResponse(distanceResponse, freight);
  }

  private DistanceResponse fetchDistanceAndDuration(LocationRequest locationRequest) {
    double originLat = locationRequest.origin().lat();
    double originLng = locationRequest.origin().lng();
    double destLat = locationRequest.destination().lat();
    double destLng = locationRequest.destination().lng();

    return getDistance(originLat, originLng, destLat, destLng);
  }

  private FreightCalculationRequest createFreightRequest(DistanceResponse distanceResponse) {
    String distanceKm = extractNumericValue(distanceResponse.distance());
    String durationMinutes = extractNumericValue(distanceResponse.duration());

    return new FreightCalculationRequest(distanceKm, durationMinutes);
  }

  private DistanceFreightResponse buildDistanceFreightResponse(
      DistanceResponse distanceResponse, double freight) {
    return new DistanceFreightResponse(
        distanceResponse.distance(), distanceResponse.duration(), freight);
  }

  private String extractNumericValue(String value) {
    return value.replaceAll("[^\\d.,]", "").replace(",", ".");
  }

  public DistanceResponse getDistance(
      double originLat, double originLng, double destLat, double destLng) {
    String url = buildApiUrl(originLat, originLng, destLat, destLng);
    String response = fetchApiResponse(url);

    return parseDistanceResponse(response);
  }

  private String buildApiUrl(double originLat, double originLng, double destLat, double destLng) {
    return UriComponentsBuilder.newInstance()
        .uri(java.net.URI.create(API_URL))
        .queryParam("origins", originLat + "," + originLng)
        .queryParam("destinations", destLat + "," + destLng)
        .queryParam("key", apiKey)
        .build()
        .toUriString();
  }

  private String fetchApiResponse(String url) {
    RestTemplate restTemplate = new RestTemplate();
    return restTemplate.getForObject(url, String.class);
  }

  private DistanceResponse parseDistanceResponse(String response) {
    try {
      JSONObject jsonResponse = new JSONObject(response);
      JSONObject row = jsonResponse.getJSONArray("rows").getJSONObject(0);
      JSONObject element = row.getJSONArray("elements").getJSONObject(0);

      String distance = element.getJSONObject("distance").getString("text");
      String duration = element.getJSONObject("duration").getString("text");

      return new DistanceResponse(distance, duration);
    } catch (Exception e) {
      throw new DistanceException(
          "Erro ao analisar a resposta da API de distância: " + e.getMessage());
    }
  }
}
