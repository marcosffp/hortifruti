package com.hortifruti.sl.hortifruti.util;

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
      System.out.println(new PDFTextStripper().getText(document));
      return new PDFTextStripper().getText(document);
    }
  }
}
