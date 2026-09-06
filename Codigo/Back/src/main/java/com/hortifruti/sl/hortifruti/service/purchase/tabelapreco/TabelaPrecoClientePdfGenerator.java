package com.hortifruti.sl.hortifruti.service.purchase.tabelapreco;

import com.hortifruti.sl.hortifruti.service.invoice.tax.PdfReportSupport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.stereotype.Component;

/**
 * PDF equivalente ao CSV de {@link TabelaPrecoClienteExportService} — mesmo layout de {@code Lista
 * maior - TABELA DE PRECOS LLINEA.pdf}: tabela simples de 3 colunas iguais, sem seções (por isso
 * segue o padrão de {@code service.invoice.tax.sales.SalesPdfGenerator}/{@link PdfReportSupport},
 * não o padrão multi-seção de {@code service.finance.AbstractPdfPageWriter}).
 */
@Component
public class TabelaPrecoClientePdfGenerator {

  private static final String[] CABECALHO = {"COD", "PRODUTO", "KG / PREÇO"};

  public byte[] gerarPdf(List<TabelaPrecoClienteExportService.LinhaTabelaPreco> linhas)
      throws IOException {
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
      PdfReportSupport.addText(contentStream, leftMargin, yPosition, "TABELA DE PREÇOS DO CLIENTE");
      yPosition -= lineHeight * 2;

      contentStream.setFont(PdfReportSupport.FONT_REGULAR, 10);
      PdfReportSupport.drawTableHeader(
          contentStream, leftMargin, yPosition, tableWidth, cellHeight, CABECALHO);
      yPosition -= cellHeight;

      for (TabelaPrecoClienteExportService.LinhaTabelaPreco linha : linhas) {
        if (yPosition < bottomMargin) {
          contentStream.close();
          page = new PDPage();
          document.addPage(page);
          contentStream = new PDPageContentStream(document, page);
          yPosition = PdfReportSupport.START_Y;
          PdfReportSupport.drawTableHeader(
              contentStream, leftMargin, yPosition, tableWidth, cellHeight, CABECALHO);
          yPosition -= cellHeight;
        }

        PdfReportSupport.drawTableRow(
            contentStream,
            leftMargin,
            yPosition,
            tableWidth,
            cellHeight,
            new String[] {
              linha.codigo(),
              linha.descricao(),
              linha.preco() == null ? "" : PdfReportSupport.formatValueComma(linha.preco())
            });
        yPosition -= cellHeight;
      }

      contentStream.close();

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        document.save(outputStream);
        return outputStream.toByteArray();
      }
    }
  }
}
