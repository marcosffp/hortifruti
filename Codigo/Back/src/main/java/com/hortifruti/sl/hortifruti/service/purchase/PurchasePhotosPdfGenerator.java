package com.hortifruti.sl.hortifruti.service.purchase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

/**
 * Junta as fotos de comprovante das compras de um agrupamento em um único PDF (uma foto por
 * página, escalada para caber na página em A4 mantendo a proporção original) — mesma biblioteca
 * (PDFBox) usada pelos demais geradores de PDF do sistema, ver {@code AbstractPdfPageWriter}.
 */
@Component
public class PurchasePhotosPdfGenerator {

  private static final float MARGIN = 20f;

  public byte[] generate(List<byte[]> images) throws IOException {
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream()) {
      for (byte[] imageBytes : images) {
        addImagePage(document, imageBytes);
      }
      document.save(pdfOut);
      return pdfOut.toByteArray();
    }
  }

  private void addImagePage(PDDocument document, byte[] imageBytes) throws IOException {
    PDImageXObject image = PDImageXObject.createFromByteArray(document, imageBytes, "foto");

    PDPage page = new PDPage(PDRectangle.A4);
    document.addPage(page);

    float availableWidth = page.getMediaBox().getWidth() - 2 * MARGIN;
    float availableHeight = page.getMediaBox().getHeight() - 2 * MARGIN;
    float scale =
        Math.min(availableWidth / image.getWidth(), availableHeight / image.getHeight());

    float drawWidth = image.getWidth() * scale;
    float drawHeight = image.getHeight() * scale;
    float x = (page.getMediaBox().getWidth() - drawWidth) / 2;
    float y = (page.getMediaBox().getHeight() - drawHeight) / 2;

    try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
      cs.drawImage(image, x, y, drawWidth, drawHeight);
    }
  }
}
