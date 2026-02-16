package com.hortifruti.sl.hortifruti.model.enumeration;

import java.io.IOException;
import java.util.Arrays;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

public enum Bank {
  BANCO_DO_BRASIL,
  SICOOB,
  UNKNOWN;

  private static Bank parseBank(String bank) {
    if (bank == null) return UNKNOWN;
    String normalized = normalize(bank);

    if (normalized.matches("^(BB|BANCODOBRASIL|BANCOBRASIL).*$")) {
      return BANCO_DO_BRASIL;
    }
    if (normalized.contains("SICOOB")) {
      return SICOOB;
    }
    return Arrays.stream(Bank.values())
        .filter(b -> normalized.equals(normalize(b.name())))
        .findFirst()
        .orElse(UNKNOWN);
  }

  public static Bank parseBank(String fileName, MultipartFile file) {
    // Primeiro tenta identificar pelo nome do arquivo
    Bank bankFromName = parseBank(fileName);

    // Se não conseguiu identificar pelo nome e o arquivo é PDF, tenta ler o conteúdo
    if (bankFromName == UNKNOWN && fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
      try {
        Bank bankFromContent = parseBankFromPdfContent(file);
        return bankFromContent;
      } catch (Exception e) {
        // Se houver erro ao ler o PDF, assume que é Banco do Brasil
        System.err.println(
            "Erro ao ler conteúdo do PDF, assumindo Banco do Brasil: " + e.getMessage());
        return BANCO_DO_BRASIL;
      }
    }

    return bankFromName;
  }

  private static Bank parseBankFromPdfContent(MultipartFile file) throws IOException {
    try (PDDocument document = Loader.loadPDF(file.getBytes())) {
      PDFTextStripper pdfStripper = new PDFTextStripper();

      // Verifica apenas a primeira página
      pdfStripper.setStartPage(1);
      pdfStripper.setEndPage(1);
      String firstPageText = pdfStripper.getText(document);
      String normalizedText = normalize(firstPageText);

      // Se encontrar SICOOB na primeira página, é SICOOB
      if (normalizedText.contains("SICOOB")) {
        return SICOOB;
      }

      // Se não encontrou SICOOB, é Banco do Brasil
      return BANCO_DO_BRASIL;

    } catch (IOException e) {
      throw new IOException("Erro ao extrair texto do PDF", e);
    }
  }

  private static String normalize(String value) {
    return value.trim().toUpperCase().replaceAll("[\\s_\\-.]+", "");
  }
}
