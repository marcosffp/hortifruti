package com.hortifruti.sl.hortifruti.controller;

import com.hortifruti.sl.hortifruti.model.Transaction;
import com.hortifruti.sl.hortifruti.service.TransactionService;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionService transactionService;

  @PostMapping("/import")
  public ResponseEntity<List<Transaction>> importarExtrato(
      @RequestParam("file") MultipartFile file) {
    try {
      List<Transaction> transactions = transactionService.importarExtrato(file);
      return ResponseEntity.ok(transactions);
    } catch (IOException e) {
      return ResponseEntity.badRequest().build();
    }
  }
}
