package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.dto.GroupedProductsResponse;
import com.hortifruti.sl.hortifruti.model.Purchase;
import com.hortifruti.sl.hortifruti.service.purchase.PurchaseService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/purchases")
@AllArgsConstructor
public class PurchaseController {

  private static final Logger logger = LoggerFactory.getLogger(PurchaseController.class);

  private final PurchaseService purchaseService;

  @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Purchase> processPurchase(@RequestParam("file") MultipartFile file) {
    try {
      Purchase purchase = purchaseService.processPurchaseFile(file);
      return ResponseEntity.ok(purchase);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/client/products")
  public ResponseEntity<GroupedProductsResponse> getGroupedProductsByClientAndPeriod(
      @RequestParam("clientId") Long clientId,
      @RequestParam("startDate") String startDateStr,
      @RequestParam("endDate") String endDateStr) {

    try {
      DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
      LocalDateTime startDate = LocalDateTime.parse(startDateStr, formatter);
      LocalDateTime endDate = LocalDateTime.parse(endDateStr, formatter);

      GroupedProductsResponse response =
          purchaseService.getGroupedProductsByClientAndPeriod(clientId, startDate, endDate);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      logger.error("Erro ao buscar produtos agrupados: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
