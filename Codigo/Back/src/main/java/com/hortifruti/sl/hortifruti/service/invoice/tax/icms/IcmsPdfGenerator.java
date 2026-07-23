package com.hortifruti.sl.hortifruti.service.invoice.tax.icms;

import com.hortifruti.sl.hortifruti.dto.invoice.tax.icms.IcmsSalesReport;
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
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IcmsPdfGenerator {

  @Value("${company.name}")
  private String companyName;

  @Value("${company.state.registration}")
  private String stateRegistration;

  @Value("${company.cnpj}")
  private String companyCnpj;

  public byte[] generateIcmsReportPdf(IcmsSalesReport report, LocalDate start, LocalDate end)
      throws IOException {

    String periodStart = start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    String periodEnd = end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    float leftMargin = PdfReportSupport.LEFT_MARGIN;
    float tableWidth = PdfReportSupport.TABLE_WIDTH;
    float cellHeight = PdfReportSupport.CELL_HEIGHT;
    float lineHeight = PdfReportSupport.LINE_HEIGHT;
    float bottomMargin = PdfReportSupport.BOTTOM_MARGIN;

    String[] headers = {
      "CFOP", "Valores Cont.", "Base de Cál.", "Imposto Deb.", "Isen ou N/Trib.", "Outras"
    };

    try (PDDocument document = new PDDocument()) {

      PDPage page = new PDPage();
      document.addPage(page);

      PDPageContentStream contentStream = new PDPageContentStream(document, page);

      float yPosition = PdfReportSupport.START_Y;

      contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
      PdfReportSupport.addText(
          contentStream, leftMargin, yPosition, "Registro de Apuração de ICMS");
      yPosition -= lineHeight * 2;

      contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);

      PdfReportSupport.addText(contentStream, leftMargin, yPosition, "FIRMA: " + companyName);
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream, leftMargin, yPosition, "INSCRIÇÃO ESTADUAL: " + stateRegistration);
      yPosition -= lineHeight;

      PdfReportSupport.addText(contentStream, leftMargin, yPosition, "CNPJ: " + companyCnpj);
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream, leftMargin, yPosition, "PERÍODO: " + periodStart + " a " + periodEnd);
      yPosition -= lineHeight * 2;

      contentStream.setLineWidth(1);
      contentStream.moveTo(leftMargin, yPosition);
      contentStream.lineTo(leftMargin + tableWidth, yPosition);
      contentStream.stroke();
      yPosition -= lineHeight;

      contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
      PdfReportSupport.addText(contentStream, leftMargin, yPosition, "SAÍDAS");
      yPosition -= lineHeight;

      PdfReportSupport.drawTableHeader(
          contentStream, leftMargin, yPosition, tableWidth, cellHeight, headers);
      yPosition -= cellHeight;

      BigDecimal subtotalOutras = BigDecimal.ZERO;

      for (Map.Entry<String, BigDecimal> entry : report.valoresPorCfop().entrySet()) {

        if (yPosition < bottomMargin) {

          contentStream.close();

          page = new PDPage();
          document.addPage(page);

          contentStream = new PDPageContentStream(document, page);

          yPosition = PdfReportSupport.START_Y;

          PdfReportSupport.drawTableHeader(
              contentStream, leftMargin, yPosition, tableWidth, cellHeight, headers);
          yPosition -= cellHeight;
        }

        PdfReportSupport.drawTableRow(
            contentStream,
            leftMargin,
            yPosition,
            tableWidth,
            cellHeight,
            new String[] {
              entry.getKey(),
              formatValue(entry.getValue()),
              "0",
              "0",
              "0",
              formatValue(entry.getValue())
            });

        subtotalOutras = subtotalOutras.add(entry.getValue());
        yPosition -= cellHeight;
      }

      PdfReportSupport.drawTableRow(
          contentStream,
          leftMargin,
          yPosition,
          tableWidth,
          cellHeight,
          new String[] {
            "Subtotal", formatValue(subtotalOutras), "0", "0", "0", formatValue(subtotalOutras)
          });

      yPosition -= cellHeight;

      PdfReportSupport.drawTableRow(
          contentStream,
          leftMargin,
          yPosition,
          tableWidth,
          cellHeight,
          new String[] {
            "Total Geral", formatValue(subtotalOutras), "0", "0", "0", formatValue(subtotalOutras)
          });

      yPosition -= cellHeight * 2;

      contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

      PdfReportSupport.addText(contentStream, leftMargin, yPosition, "Legenda:");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream,
          leftMargin,
          yPosition,
          "CFOP: Código que identifica o tipo de operação fiscal.");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream,
          leftMargin,
          yPosition,
          "Valores Contábeis: Valor total registrado da operação.");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream,
          leftMargin,
          yPosition,
          "Base de Cálculo: Valor sobre o qual o ICMS é calculado.");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream, leftMargin, yPosition, "Imposto Debitado: Valor do ICMS devido.");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream,
          leftMargin,
          yPosition,
          "Isentas ou Não Tributadas: Operações sem incidência de ICMS.");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream,
          leftMargin,
          yPosition,
          "Outras: Valores que não entram na base de cálculo.");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream, leftMargin, yPosition, "Subtotal: Soma parcial das operações.");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream,
          leftMargin,
          yPosition,
          "Total Geral: Soma total das operações do período.");

      contentStream.close();

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        document.save(outputStream);
        return outputStream.toByteArray();
      }
    }
  }

  private String formatValue(Object value) {

    if (value == null) {
      return "0";
    }

    if (value instanceof BigDecimal) {
      BigDecimal bd = (BigDecimal) value;
      return bd.compareTo(BigDecimal.ZERO) == 0 ? "0" : bd.toString();
    }

    return value.toString();
  }
}
