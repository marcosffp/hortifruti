package com.hortifruti.sl.hortifruti.service.invoice.tax;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Desenho compartilhado dos relatórios fiscais em PDF (ICMS, pagamento, registro de saídas, vendas)
 * — todos usam o mesmo layout de página única (margem, largura de tabela, altura de célula) e a
 * mesma grade de tabela com colunas de largura igual; só o de vendas usa larguras de coluna
 * variáveis e mantém seu próprio desenho de tabela por isso.
 */
public final class PdfReportSupport {

  public static final float LEFT_MARGIN = 50;
  public static final float TABLE_WIDTH = 500;
  public static final float CELL_HEIGHT = 25;
  public static final float LINE_HEIGHT = 20;
  public static final float BOTTOM_MARGIN = 100;
  public static final float START_Y = 750;

  private PdfReportSupport() {}

  public static void addText(PDPageContentStream contentStream, float x, float y, String text)
      throws IOException {
    contentStream.beginText();
    contentStream.newLineAtOffset(x, y);
    contentStream.showText(text);
    contentStream.endText();
  }

  public static void drawTableHeader(
      PDPageContentStream contentStream,
      float x,
      float y,
      float width,
      float height,
      String[] headers)
      throws IOException {
    drawEqualWidthRow(contentStream, x, y, width, height, headers, true);
  }

  public static void drawTableRow(
      PDPageContentStream contentStream,
      float x,
      float y,
      float width,
      float height,
      String[] values)
      throws IOException {
    drawEqualWidthRow(contentStream, x, y, width, height, values, false);
  }

  private static void drawEqualWidthRow(
      PDPageContentStream contentStream,
      float x,
      float y,
      float width,
      float height,
      String[] cells,
      boolean bold)
      throws IOException {
    float cellWidth = width / cells.length;

    for (int i = 0; i < cells.length; i++) {
      contentStream.addRect(x + (cellWidth * i), y, cellWidth, -height);
      contentStream.stroke();

      contentStream.beginText();
      contentStream.setFont(
          new PDType1Font(
              bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA),
          10);
      contentStream.newLineAtOffset(x + (cellWidth * i) + 15, y - height + 10);
      contentStream.showText(cells[i]);
      contentStream.endText();
    }
  }

  public static String formatValueComma(BigDecimal value) {
    return value == null
        ? "0,00"
        : value.setScale(2, RoundingMode.HALF_UP).toString().replace(".", ",");
  }
}
