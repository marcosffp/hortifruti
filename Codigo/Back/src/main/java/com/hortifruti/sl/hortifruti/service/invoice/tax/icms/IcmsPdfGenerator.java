package com.hortifruti.sl.hortifruti.service.invoice.tax.icms;

import com.hortifruti.sl.hortifruti.dto.invoice.IcmsSalesReport;
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
        contentStream.showText("Registro de Apuração de ICMS");
        contentStream.endText();
        yPosition -= lineHeight * 2;

        contentStream.setFont(PDType1Font.HELVETICA, 12);
        addText(contentStream, leftMargin, yPosition, "FIRMA: " + companyName);
        yPosition -= lineHeight;
        addText(contentStream, leftMargin, yPosition, "INSCRIÇÃO ESTADUAL: " + stateRegistration);
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

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
        addText(contentStream, leftMargin, yPosition, "SAÍDAS");
        yPosition -= lineHeight;

        drawTableHeader(
            contentStream,
            leftMargin,
            yPosition,
            tableWidth,
            cellHeight,
            new String[] {
              "CFOP", "Valores Cont.", "Base de Cál.", "Imposto Deb.", "Isen ou N/Trib.", "Outras"
            });
        yPosition -= cellHeight;

        BigDecimal subtotalOutras = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : report.valoresPorCfop().entrySet()) {
          drawTableRow(
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

        drawTableRow(
            contentStream,
            leftMargin,
            yPosition,
            tableWidth,
            cellHeight,
            new String[] {
              "Subtotal", formatValue(subtotalOutras), "0", "0", "0", formatValue(subtotalOutras)
            });
        yPosition -= cellHeight;

        drawTableRow(
            contentStream,
            leftMargin,
            yPosition,
            tableWidth,
            cellHeight,
            new String[] {
              "Total Geral", formatValue(subtotalOutras), "0", "0", "0", formatValue(subtotalOutras)
            });
        yPosition -= cellHeight * 2;

        // Legenda explicativa
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        addText(contentStream, leftMargin, yPosition, "Legenda:");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "CFOP: Código que identifica o tipo de operação fiscal (ex.: venda, devolução, transferência).");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Valores Contábeis: Valor total registrado da operação.");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Base de Cálculo: Valor sobre o qual o ICMS é calculado.");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Imposto Debitado: Valor do ICMS devido sobre as operações de saída.");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Isentas ou Não Tributadas: Operações que não geram cobrança de ICMS por isenção ou não incidência.");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Outras: Valores que não entram na base de cálculo do imposto, mas são informados para controle.");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Subtotal: Soma parcial das operações de um mesmo grupo (ex.: dentro do estado).");
        yPosition -= lineHeight;
        addText(
            contentStream,
            leftMargin,
            yPosition,
            "Total Geral: Soma total de todas as operações apuradas no período.");
      }

      // Retornar o documento como byte[]
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
      contentStream.newLineAtOffset(
          x + (cellWidth * i) + 15, y - height + 10); // Ajustado espaçamento interno
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
      contentStream.newLineAtOffset(
          x + (cellWidth * i) + 15, y - height + 10); // Ajustado espaçamento interno
      contentStream.showText(values[i]);
      contentStream.endText();
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
