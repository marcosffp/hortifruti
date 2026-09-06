package com.hortifruti.sl.hortifruti.service.invoice.tax.payment;

import com.hortifruti.sl.hortifruti.service.invoice.tax.PdfReportSupport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
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

    float leftMargin = PdfReportSupport.LEFT_MARGIN;
    float tableWidth = PdfReportSupport.TABLE_WIDTH;
    float cellHeight = PdfReportSupport.CELL_HEIGHT;
    float lineHeight = PdfReportSupport.LINE_HEIGHT;
    float bottomMargin = PdfReportSupport.BOTTOM_MARGIN;

    try (PDDocument document = new PDDocument()) {

      PDPage page = new PDPage();
      document.addPage(page);

      PDPageContentStream contentStream = new PDPageContentStream(document, page);

      float yPosition = PdfReportSupport.START_Y;

      contentStream.setFont(PdfReportSupport.FONT_BOLD, 16);
      PdfReportSupport.addText(
          contentStream, leftMargin, yPosition, "RESUMO DE VENDAS POR FORMA DE PAGAMENTO");
      yPosition -= lineHeight * 2;

      contentStream.setFont(PdfReportSupport.FONT_REGULAR, 12);

      PdfReportSupport.addText(
          contentStream, leftMargin, yPosition, "Filial \"igual\": 1 " + companyName);
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream,
          leftMargin,
          yPosition,
          "Data Envio \"entre\": " + periodStart + " a " + periodEnd);
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream,
          leftMargin,
          yPosition,
          "Modelo \"iniciado por\": 55 NOTA FISCAL ELETRÔNICA - NF-E");
      yPosition -= lineHeight;

      PdfReportSupport.addText(contentStream, leftMargin, yPosition, "Situação \"igual\": ATIVAS");
      yPosition -= lineHeight * 2;

      contentStream.setLineWidth(1);
      contentStream.moveTo(leftMargin, yPosition);
      contentStream.lineTo(leftMargin + tableWidth, yPosition);
      contentStream.stroke();
      yPosition -= lineHeight;

      PdfReportSupport.drawTableHeader(
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

        if (yPosition < bottomMargin) {

          contentStream.close();

          page = new PDPage();
          document.addPage(page);

          contentStream = new PDPageContentStream(document, page);

          yPosition = PdfReportSupport.START_Y;

          PdfReportSupport.drawTableHeader(
              contentStream,
              leftMargin,
              yPosition,
              tableWidth,
              cellHeight,
              new String[] {"FORMA DE PAGAMENTO", "TOTAL"});

          yPosition -= cellHeight;
        }

        PdfReportSupport.drawTableRow(
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

      PdfReportSupport.drawTableRow(
          contentStream,
          leftMargin,
          yPosition,
          tableWidth,
          cellHeight,
          new String[] {"Registros: " + recordCount, "TOTAL: " + formatValue(totalAmount)});

      yPosition -= cellHeight * 2;

      PdfReportSupport.drawTableHeader(
          contentStream,
          leftMargin,
          yPosition,
          tableWidth,
          cellHeight,
          new String[] {"TIPO DE PAGAMENTO", "TOTAL"});

      yPosition -= cellHeight;

      PdfReportSupport.drawTableRow(
          contentStream,
          leftMargin,
          yPosition,
          tableWidth,
          cellHeight,
          new String[] {"DINHEIRO", formatValue(totalAmount)});

      yPosition -= cellHeight;

      PdfReportSupport.drawTableRow(
          contentStream,
          leftMargin,
          yPosition,
          tableWidth,
          cellHeight,
          new String[] {"TOTAL", formatValue(totalAmount)});

      contentStream.close();

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        document.save(outputStream);
        return outputStream.toByteArray();
      }
    }
  }

  private String formatValue(BigDecimal value) {
    return PdfReportSupport.formatValueComma(value);
  }
}
