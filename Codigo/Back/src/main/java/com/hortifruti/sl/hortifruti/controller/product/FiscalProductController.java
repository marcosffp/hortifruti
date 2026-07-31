package com.hortifruti.sl.hortifruti.controller.product;

import com.hortifruti.sl.hortifruti.dto.product.FiscalProductResponse;
import com.hortifruti.sl.hortifruti.service.product.FiscalProductService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fiscal-products")
@AllArgsConstructor
public class FiscalProductController {

  private final FiscalProductService fiscalProductService;

  @GetMapping
  public ResponseEntity<List<FiscalProductResponse>> getAllFiscalProducts() {
    return ResponseEntity.ok(fiscalProductService.listAll());
  }
}
