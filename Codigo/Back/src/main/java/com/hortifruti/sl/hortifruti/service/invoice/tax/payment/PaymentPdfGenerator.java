package com.hortifruti.sl.hortifruti.service.invoice.tax.payment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentPdfGenerator {

  @Value("${company.name}")
  private String companyName;

  @Value("${company.cnpj}")
  private String companyCnpj;

  public byte[] generateSummaryByPaymentPdf(
      Map<String, BigDecimal> paymentSummary, LocalDate startDate, LocalDate endDate)
      throws IOException {
    String periodStart = startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    String periodEnd = endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    try (PDDocument document = new PDDocument()) {
      PDPage page = new PDPage();
      document.addPage(page);

      try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
        float yPosition = 750;
        float leftMargin = 50;
        float tableWidth = 500;
        float cellHeight = 25;
        float lineHeight = 20;

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("RESUMO DE VENDAS POR FORMA DE PAGAMENTO");
        contentStream.endText();
        yPosition -= lineHeight * 2;

        contentStream.setFont(PDType1Font.HELVETICA, 12);
        addText(contentStream, leftMargin, yPosition, "Filial \"igual\": 1 " + companyName);
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Data Envio \"entre\": " + periodStart + " a " + periodEnd);
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Modelo \"iniciado por\": 55 NOTA FISCAL ELETRÔNICA - NF-E");
        yPosition -= lineHeight;
        addText(contentStream, leftMargin, yPosition, "Situação \"igual\": ATIVAS");
        yPosition -= lineHeight * 2;

        contentStream.setLineWidth(1);
        contentStream.moveTo(leftMargin, yPosition);
        contentStream.lineTo(leftMargin + tableWidth, yPosition);
        contentStream.stroke();
        yPosition -= lineHeight;

        drawTableHeader(
            contentStream,
            leftMargin,
            yPosition,
            tableWidth,
            cellHeight,
            new String[] {"FORMA DE PAGAMENTO", "TOTAL"});
        yPosition -= cellHeight;

        BigDecimal totalAmount = BigDecimal.ZERO;
        int recordCount = 0;
        for (Map.Entry<String, BigDecimal> entry : paymentSummary.entrySet()) {
          drawTableRow(
              contentStream,
              leftMargin,
              yPosition,
              tableWidth,
              cellHeight,
              new String[] {entry.getKey(), formatValue(entry.getValue())});
          totalAmount = totalAmount.add(entry.getValue());
          recordCount++;
          yPosition -= cellHeight;
        }

        drawTableRow(
            contentStream,
            leftMargin,
            yPosition,
            tableWidth,
            cellHeight,
            new String[] {"Registros: " + recordCount, "TOTAL: " + formatValue(totalAmount)});
        yPosition -= cellHeight * 2;

        drawTableHeader(
            contentStream,
            leftMargin,
            yPosition,
            tableWidth,
            cellHeight,
            new String[] {"TIPO DE PAGAMENTO", "TOTAL"});
        yPosition -= cellHeight;

        drawTableRow(
            contentStream,
            leftMargin,
            yPosition,
            tableWidth,
            cellHeight,
            new String[] {"DINHEIRO", formatValue(totalAmount)});
        yPosition -= cellHeight;

        drawTableRow(
            contentStream,
            leftMargin,
            yPosition,
            tableWidth,
            cellHeight,
            new String[] {"TOTAL", formatValue(totalAmount)});
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
    float cellWidth = width / headers.length;

    for (int i = 0; i < headers.length; i++) {
      contentStream.addRect(x + (cellWidth * i), y, cellWidth, -height);
      contentStream.stroke();

      contentStream.beginText();
      contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
      contentStream.newLineAtOffset(x + (cellWidth * i) + 15, y - height + 10);
      contentStream.showText(headers[i]);
      contentStream.endText();
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
    float cellWidth = width / values.length;

    for (int i = 0; i < values.length; i++) {
      contentStream.addRect(x + (cellWidth * i), y, cellWidth, -height);
      contentStream.stroke();

      contentStream.beginText();
      contentStream.setFont(PDType1Font.HELVETICA, 10);
      contentStream.newLineAtOffset(x + (cellWidth * i) + 15, y - height + 10);
      contentStream.showText(values[i]);
      contentStream.endText();
    }
  }

  private String formatValue(BigDecimal value) {
    return value == null
        ? "0,00"
        : value.setScale(2, java.math.RoundingMode.HALF_UP).toString().replace(".", ",");
  }
}
