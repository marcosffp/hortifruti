package com.hortifruti.sl.hortifruti.service.invoice.tax.sales;

import com.hortifruti.sl.hortifruti.dto.invoice.SalesSummaryDetails;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SalesPdfGenerator {

  @Value("${company.name}")
  private String companyName;

  @Value("${company.cnpj}")
  private String companyCnpj;

  public byte[] generateSalesReportPdf(
      List<SalesSummaryDetails> salesSummaries, LocalDate startDate, LocalDate endDate)
      throws IOException {
    String periodStart = startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    String periodEnd = endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    try (PDDocument document = new PDDocument()) {
      PDPage page = new PDPage();
      document.addPage(page);

      try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
        float yPosition = 750;
        float leftMargin = 10;
        float tableWidth = 500;
        float cellHeight = 25;
        float lineHeight = 20;

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("RELAÇÃO DE VENDAS");
        contentStream.endText();
        yPosition -= lineHeight * 2;

        contentStream.setFont(PDType1Font.HELVETICA, 12);
        addText(contentStream, leftMargin, yPosition, "Filial: " + companyName);
        yPosition -= lineHeight;
        addText(contentStream, leftMargin, yPosition, "CNPJ: " + companyCnpj);
        yPosition -= lineHeight;
        addText(
            contentStream, leftMargin, yPosition, "Período: " + periodStart + " a " + periodEnd);
        yPosition -= lineHeight * 2;

        drawTableHeader(
            contentStream,
            leftMargin,
            yPosition,
            tableWidth,
            cellHeight,
            new String[] {
              "Número",
              "Mod",
              "Data",
              "Envio",
              "Cliente",
              "Subtotal",
              "Desconto",
              "Acréscimo",
              "Total - R$"
            });
        yPosition -= cellHeight;

        for (SalesSummaryDetails summary : salesSummaries) {
          drawTableRow(
              contentStream,
              leftMargin,
              yPosition,
              tableWidth,
              cellHeight,
              new String[] {
                summary.numero(),
                "55",
                summary.data(),
                summary.envio(),
                summary.cliente(),
                formatValue(summary.subtotal()),
                formatValue(summary.desconto()),
                formatValue(summary.acrescimo()),
                formatValue(summary.total())
              });
          yPosition -= cellHeight;
        }
      }

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        document.save(outputStream);
        return outputStream.toByteArray();
      }
    }
  }

  private void addText(PDPageContentStream contentStream, float x, float y, String text)
      throws IOException {
    contentStream.beginText();
    contentStream.newLineAtOffset(x, y);
    contentStream.showText(text);
    contentStream.endText();
  }

  private void drawTableHeader(
      PDPageContentStream contentStream,
      float x,
      float y,
      float width,
      float height,
      String[] headers)
      throws IOException {
    float[] columnWidths = {
      50, 40, 70, 70, 120, 60, 60, 60, 60
    }; // Define largura específica para cada coluna

    for (int i = 0; i < headers.length; i++) {
      contentStream.addRect(x, y, columnWidths[i], -height);
      contentStream.stroke();

      contentStream.beginText();
      contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
      contentStream.newLineAtOffset(x + 5, y - height + 10);
      contentStream.showText(headers[i]);
      contentStream.endText();

      x += columnWidths[i];
    }
  }

  private void drawTableRow(
      PDPageContentStream contentStream,
      float x,
      float y,
      float width,
      float height,
      String[] values)
      throws IOException {
    float[] columnWidths = {50, 40, 70, 70, 120, 60, 60, 60, 60};

    for (int i = 0; i < values.length; i++) {
      contentStream.addRect(x, y, columnWidths[i], -height);
      contentStream.stroke();

      contentStream.beginText();
      contentStream.setFont(PDType1Font.HELVETICA, 10);
      contentStream.newLineAtOffset(x + 5, y - height + 10);

      String valueToShow =
          i == 4 && values[i].length() > 17 ? values[i].substring(0, 17) + "..." : values[i];
      contentStream.showText(valueToShow);
      contentStream.endText();

      x += columnWidths[i];
    }
  }

  private String formatValue(BigDecimal value) {
    return value == null
        ? "0,00"
        : value.setScale(2, java.math.RoundingMode.HALF_UP).toString().replace(".", ",");
  }
}
