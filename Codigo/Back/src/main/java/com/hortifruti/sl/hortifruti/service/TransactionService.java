package com.hortifruti.sl.hortifruti.service;

import com.hortifruti.sl.hortifruti.model.Transaction;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class TransactionService {

  private List<Transaction> parseBancoBrasil(String texto) {
    List<Transaction> lancamentos = new ArrayList<>();
    String[] linhas = texto.split("\n");

    Pattern padraoTransacao =
        Pattern.compile(
            "^(\\d{2}/\\d{2}/\\d{4})\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(.+?)\\s+([\\d.,]+)\\s+([CD])$");
    Pattern padraoDetalhe = Pattern.compile("^\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}\\s+.+");

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    for (int i = 0; i < linhas.length; i++) {
      String linhaAtual = linhas[i].trim();
      Matcher matcher = padraoTransacao.matcher(linhaAtual);

      if (matcher.find()) {
        String data = matcher.group(1);
        String codigo = matcher.group(4);
        String descricao = matcher.group(5).trim();
        String valor = matcher.group(6);
        String tipoSaldo = matcher.group(7);

        if (i + 1 < linhas.length) {
          String proximaLinha = linhas[i + 1].trim();
          if (padraoDetalhe.matcher(proximaLinha).matches()) {
            descricao += " - " + proximaLinha;
            i++;
          }
        }

        // Conversão dos campos
        LocalDate transactionDate = LocalDate.parse(data, formatter);
        String document = codigo; // ou combine agencia/lote/codigo se necessário
        String history = descricao;
        BigDecimal amount = new BigDecimal(valor.replace(".", "").replace(",", "."));
        if ("D".equalsIgnoreCase(tipoSaldo)) {
          amount = amount.negate();
        }

        Transaction transaction =
            Transaction.builder()
                .transactionDate(transactionDate)
                .document(document)
                .history(history)
                .amount(amount)
                .build();

        lancamentos.add(transaction);
      }
    }

    return lancamentos;
  }

  private String extractPdf(MultipartFile file) throws IOException {
    try (PDDocument document = PDDocument.load(file.getInputStream())) {
      PDFTextStripper pdfStripper = new PDFTextStripper();
      return pdfStripper.getText(document);
    }
  }

  public List<Transaction> importarExtrato(MultipartFile file) throws IOException {
    String texto = extractPdf(file);
    return parseBancoBrasil(texto);
  }
}
