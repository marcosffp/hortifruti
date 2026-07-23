package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.freight.DistanceFreightResponse;
import com.hortifruti.sl.hortifruti.dto.freight.FreightConfigDTO;
import com.hortifruti.sl.hortifruti.dto.freight.LocationRequest;
import com.hortifruti.sl.hortifruti.service.freight.DistanceMatrixService;
import com.hortifruti.sl.hortifruti.service.freight.FreightPropertiesService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/distance")
@AllArgsConstructor
public class DistanceController {

  private final DistanceMatrixService distanceMatrixService;
  private final FreightPropertiesService freightPropertiesService;

  @PostMapping
  public DistanceFreightResponse getDistance(@RequestBody LocationRequest locationRequest) {
    return distanceMatrixService.calculateDistanceAndFreight(locationRequest);
  }

  @GetMapping("/freight-config")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<FreightConfigDTO> getFreightConfig() {
    return ResponseEntity.ok(freightPropertiesService.getFreightConfig());
  }

  @PatchMapping("/freight-config")
  @PreAuthorize("hasRole('MANAGER')")
  public ResponseEntity<FreightConfigDTO> updateFreightConfig(@RequestBody FreightConfigDTO dto) {
    return ResponseEntity.ok(freightPropertiesService.updateFreightConfig(dto));
  }
}
