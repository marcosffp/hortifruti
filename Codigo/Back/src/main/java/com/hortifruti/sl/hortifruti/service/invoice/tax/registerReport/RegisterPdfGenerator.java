package com.hortifruti.sl.hortifruti.service.invoice.tax.registerReport;

import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceSummaryDetails;
import com.hortifruti.sl.hortifruti.service.invoice.tax.PdfReportSupport;
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
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RegisterPdfGenerator {

  @Value("${company.name}")
  private String companyName;

  @Value("${company.cnpj}")
  private String companyCnpj;

  public byte[] generateRegisterReportPdf(
      List<InvoiceSummaryDetails> invoiceSummaries, LocalDate startDate, LocalDate endDate)
      throws IOException {

    String periodStart = startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    String periodEnd = endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    float leftMargin = PdfReportSupport.LEFT_MARGIN;
    float tableWidth = PdfReportSupport.TABLE_WIDTH;
    float cellHeight = PdfReportSupport.CELL_HEIGHT;
    float lineHeight = PdfReportSupport.LINE_HEIGHT;
    float bottomMargin = PdfReportSupport.BOTTOM_MARGIN;

    String[] headers = {"Espécie", "Série", "Dia", "UF", "Valor", "Cod. Fiscal", "Aliq.", "Outras"};

    try (PDDocument document = new PDDocument()) {

      PDPage page = new PDPage();
      document.addPage(page);

      PDPageContentStream contentStream = new PDPageContentStream(document, page);

      float yPosition = PdfReportSupport.START_Y;

      contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
      PdfReportSupport.addText(
          contentStream, leftMargin, yPosition, "Livro de Registro de Saídas - RE - Modelo P 2/A");
      yPosition -= lineHeight * 2;

      contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
      PdfReportSupport.addText(contentStream, leftMargin, yPosition, "FIRMA: " + companyName);
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

      PdfReportSupport.drawTableHeader(
          contentStream, leftMargin, yPosition, tableWidth, cellHeight, headers);
      yPosition -= cellHeight;

      for (InvoiceSummaryDetails summary : invoiceSummaries) {

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
              summary.especie(),
              summary.serie(),
              summary.dia(),
              summary.uf(),
              formatValue(summary.valor()),
              summary.predominante(),
              formatValue(summary.aliquota()),
              formatValue(summary.valor())
            });

        yPosition -= cellHeight;
      }

      yPosition -= lineHeight * 2;

      contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

      PdfReportSupport.addText(contentStream, leftMargin, yPosition, "Legenda:");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream,
          leftMargin,
          yPosition,
          "Espécie: Tipo do documento fiscal emitido (ex.: NF-e, NFC-e, CF-e, etc.).");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream,
          leftMargin,
          yPosition,
          "Série: Código que identifica a série da nota fiscal.");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream, leftMargin, yPosition, "Número: Número sequencial do documento fiscal.");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream, leftMargin, yPosition, "Dia: Data de emissão do documento.");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream, leftMargin, yPosition, "UF: Unidade Federativa de destino.");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream, leftMargin, yPosition, "Valor: Valor total do documento fiscal.");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream, leftMargin, yPosition, "Cod. Fiscal: Código CFOP da operação.");
      yPosition -= lineHeight;

      PdfReportSupport.addText(contentStream, leftMargin, yPosition, "Aliq.: Alíquota do imposto.");
      yPosition -= lineHeight;

      PdfReportSupport.addText(
          contentStream,
          leftMargin,
          yPosition,
          "Outras: Valores não enquadrados nas categorias principais.");

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
