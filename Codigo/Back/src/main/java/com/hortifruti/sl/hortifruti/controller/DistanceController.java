package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.DistanceResponse;
import com.hortifruti.sl.hortifruti.dto.LocationRequest;
import com.hortifruti.sl.hortifruti.service.DistanceMatrixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/distance")
public class DistanceController {

  @Autowired private DistanceMatrixService distanceMatrixService;

  @PostMapping
  public DistanceResponse getDistance(@RequestBody LocationRequest locationRequest) {
    double originLat = locationRequest.origin().lat();
    double originLng = locationRequest.origin().lng();
    double destLat = locationRequest.destination().lat();
    double destLng = locationRequest.destination().lng();

    // Chamando o serviço para obter a distância e o tempo
    return distanceMatrixService.getDistance(originLat, originLng, destLat, destLng);
  }
}
