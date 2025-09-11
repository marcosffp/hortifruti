package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.DistanceFreightResponse;
import com.hortifruti.sl.hortifruti.dto.DistanceResponse;
import com.hortifruti.sl.hortifruti.dto.FreightCalculationRequest;
import com.hortifruti.sl.hortifruti.dto.LocationRequest;
import com.hortifruti.sl.hortifruti.service.DistanceMatrixService;
import com.hortifruti.sl.hortifruti.service.FreightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/distance")
public class DistanceController {

  @Autowired
  private DistanceMatrixService distanceMatrixService;
  @Autowired
  private FreightService freightService;

  @PostMapping
  public DistanceFreightResponse getDistance(@RequestBody LocationRequest locationRequest) {
    double originLat = locationRequest.origin().lat();
    double originLng = locationRequest.origin().lng();
    double destLat = locationRequest.destination().lat();
    double destLng = locationRequest.destination().lng();

    // Obter distância e tempo
    DistanceResponse distanceResponse = distanceMatrixService.getDistance(originLat, originLng, destLat, destLng);

    // Extrair valores numéricos para o cálculo do frete
    String distanceKm = distanceResponse.distance().replaceAll("[^\\d.,]", "").replace(",", ".");
    String durationMinutes = distanceResponse.duration().replaceAll("[^\\d]", "");

    FreightCalculationRequest freightRequest = new FreightCalculationRequest(distanceKm, durationMinutes);
    double freight = freightService.calculateFreight(freightRequest);

    return new DistanceFreightResponse(distanceResponse.distance(), distanceResponse.duration(), freight);
  }
}
