package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.service.GoogleDirectionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/directions")
@RequiredArgsConstructor
public class GoogleMapsController {

  private final GoogleDirectionsService directionsService;

  /**
   * Calcula rota, distância, tempo e valor do frete usando a Directions API.
   * 
   * @param request objeto com origem e destino (lat/lng)
   * @return informações da rota
   */
  @PostMapping("/route")
  public GoogleDirectionsService.RouteInfo route(
      @RequestBody RouteRequestDTO request) {
    String origin = request.origin.lat + "," + request.origin.lng;
    String destination = request.destination.lat + "," + request.destination.lng;
    return directionsService.routeAndFreight(origin, destination);
  }
}