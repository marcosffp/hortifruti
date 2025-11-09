package com.hortifruti.sl.hortifruti.service.invoice.tax.registerReport;

import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceSummaryDetails;
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
        contentStream.showText("Livro de Registro de Saídas - RE - Modelo P 2/A");
        contentStream.endText();
        yPosition -= lineHeight * 2;

        contentStream.setFont(PDType1Font.HELVETICA, 12);
        addText(contentStream, leftMargin, yPosition, "FIRMA: " + companyName);
        yPosition -= lineHeight;
        addText(contentStream, leftMargin, yPosition, "CNPJ: " + companyCnpj);
        yPosition -= lineHeight;
        addText(
            contentStream, leftMargin, yPosition, "PERÍODO: " + periodStart + " a " + periodEnd);
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
            new String[] {
              "Espécie", "Série", "Dia", "UF", "Valor", "Cod. Fiscal", "Aliq.", "Outras"
            });
        yPosition -= cellHeight;

        for (InvoiceSummaryDetails summary : invoiceSummaries) {
          drawTableRow(
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
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        addText(contentStream, leftMargin, yPosition, "Legenda:");
        yPosition -= lineHeight;

        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Espécie: Tipo do documento fiscal emitido (ex.: NF-e, NFC-e, CF-e, etc.).");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Série: Código que identifica a série da nota fiscal, usado para diferenciar numerações.");
        yPosition -= lineHeight;
        addText(
            contentStream, leftMargin, yPosition, "Número: Número sequencial do documento fiscal.");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Dia: Data de emissão do documento ou dia de ocorrência da operação.");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "UF: Unidade Federativa (estado) de destino da mercadoria ou serviço.");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Valor: Valor total do documento fiscal registrado na contabilidade.");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Cod.: Fiscal Código de classificação fiscal da operação (CFOP).");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Aliq.: Alíquota aplicável do imposto (percentual de tributação).");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Outras: Valores ou operações não enquadradas nas categorias principais (ex.: ajustes, descontos, etc.).");
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
