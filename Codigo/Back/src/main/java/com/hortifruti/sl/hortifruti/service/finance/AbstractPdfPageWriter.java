package com.hortifruti.sl.hortifruti.service.finance;

import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Base comum para os "PageWriter" que desenham extratos/relatórios em PDF via PDFBox campo a campo
 * — usada por {@code SicoobExtratoPdfGenerator}, {@code BBExtratoPdfGenerator} e {@code
 * TransactionReportPdfGenerator}, que só diferem no layout de colunas e no conteúdo desenhado em
 * cada página.
 */
public abstract class AbstractPdfPageWriter {

  protected static final float MARGIN = 40;
  protected static final PDFont FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  protected static final PDFont FONT_BOLD =
      new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

  protected final PDDocument document;
  protected PDPage page;
  protected PDPageContentStream cs;
  protected float y;
  protected float pageWidth;
  protected float pageHeight;

  protected AbstractPdfPageWriter(PDDocument document) {
    this.document = document;
  }

  /** Redesenhado no topo de cada página nova, inclusive nas geradas por {@link #ensureSpace}. */
  protected abstract void drawTableHeader() throws IOException;

  protected void newPage() throws IOException {
    page = new PDPage(PDRectangle.A4);
    document.addPage(page);
    pageWidth = page.getMediaBox().getWidth();
    pageHeight = page.getMediaBox().getHeight();
    cs = new PDPageContentStream(document, page);
    y = pageHeight - MARGIN;
  }

  protected void ensureSpace() throws IOException {
    ensureSpace(30);
  }

  protected void ensureSpace(float needed) throws IOException {
    if (y < MARGIN + needed) {
      cs.close();
      newPage();
      drawTableHeader();
    }
  }

  protected void text(PDFont font, float size, float x, float yPos, String value, float[] rgb)
      throws IOException {
    if (value == null || value.isEmpty()) {
      return;
    }
    cs.beginText();
    cs.setFont(font, size);
    cs.setNonStrokingColor(
        rgb != null ? rgb[0] : 0f, rgb != null ? rgb[1] : 0f, rgb != null ? rgb[2] : 0f);
    cs.newLineAtOffset(x, yPos);
    cs.showText(value);
    cs.endText();
  }

  protected void textRightAligned(
      PDFont font, float size, float rightEdge, float yPos, String value) throws IOException {
    textRightAligned(font, size, rightEdge, yPos, value, null);
  }

  protected void textRightAligned(
      PDFont font, float size, float rightEdge, float yPos, String value, float[] rgb)
      throws IOException {
    float width = font.getStringWidth(value) / 1000 * size;
    text(font, size, rightEdge - width, yPos, value, rgb);
  }

  protected String truncate(String value, int maxLength) {
    if (value == null) return "";
    return value.length() > maxLength ? value.substring(0, maxLength - 3) + "..." : value;
  }
}
