package com.hortifruti.sl.hortifruti.util;

import com.hortifruti.sl.hortifruti.exception.PurchaseProcessingException;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PdfUtil {
  private PdfUtil() {}

  public static String extractPdfText(MultipartFile file) throws IOException {
    try (PDDocument document = PDDocument.load(file.getInputStream())) {
      return new PDFTextStripper().getText(document);
    }
  }

  public static String findValueByKeyword(String text, String keyword) {
    String[] lines = text.split("\n");
    for (String line : lines) {
      if (line.toLowerCase().contains(keyword.toLowerCase()) && line.contains(":")) {
        return line.split(":", 2)[1].trim();
      }
    }
    throw new PurchaseProcessingException(
        "Não foi possível encontrar '" + keyword + "' no documento");
  }
}
