package com.hortifruti.sl.hortifruti.service.finance;

import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.util.SicoobExtratoFormatUtil;
import com.hortifruti.sl.hortifruti.util.TransactionUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Gera o relatório consolidado de transações (BB + Sicoob juntos, em ordem cronológica) em PDF —
 * mesmo padrão de desenho de {@link BBExtratoPdfGenerator}/{@link SicoobExtratoPdfGenerator}
 * (helpers {@code text}/{@code textRightAligned}, {@code PageWriter} sem estado de instância
 * compartilhado entre chamadas).
 */
@Component
public class TransactionReportPdfGenerator {

  @Value("${company.name}")
  private String companyName;

  private static final float MARGIN = 40;
  private static final float COL_DATA_X = 0;
  private static final float COL_BANCO_X = 55;
  private static final float COL_CATEGORIA_X = 100;
  private static final float COL_HIST_X = 195;
  private static final float COL_VALOR_WIDTH = 75;
  private static final DateTimeFormatter DATA_CURTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final PDFont FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

  public byte[] generate(LocalDate dataInicio, LocalDate dataFim, List<Transaction> transacoes)
      throws IOException {
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream()) {

      new PageWriter(document, dataInicio, dataFim).write(transacoes);

      document.save(pdfOut);
      return pdfOut.toByteArray();
    }
  }

  private class PageWriter {
    private final PDDocument document;
    private final LocalDate dataInicio;
    private final LocalDate dataFim;
    private PDPage page;
    private PDPageContentStream cs;
    private float y;
    private float pageWidth;
    private float pageHeight;

    PageWriter(PDDocument document, LocalDate dataInicio, LocalDate dataFim) {
      this.document = document;
      this.dataInicio = dataInicio;
      this.dataFim = dataFim;
    }

    void write(List<Transaction> transacoes) throws IOException {
      newPage();
      drawHeader();
      drawTableHeader();

      BigDecimal totalEntradas = BigDecimal.ZERO;
      BigDecimal totalSaidas = BigDecimal.ZERO;

      for (Transaction transacao : transacoes) {
        ensureSpace();
        drawLinha(transacao);
        if (transacao.getAmount().signum() >= 0) {
          totalEntradas = totalEntradas.add(transacao.getAmount());
        } else {
          totalSaidas = totalSaidas.add(transacao.getAmount());
        }
      }

      ensureSpace(60);
      drawResumo(transacoes.size(), totalEntradas, totalSaidas);
      cs.close();
    }

    private void newPage() throws IOException {
      page = new PDPage(PDRectangle.A4);
      document.addPage(page);
      pageWidth = page.getMediaBox().getWidth();
      pageHeight = page.getMediaBox().getHeight();
      cs = new PDPageContentStream(document, page);
      y = pageHeight - MARGIN;
    }

    private void ensureSpace() throws IOException {
      ensureSpace(30);
    }

    private void ensureSpace(float needed) throws IOException {
      if (y < MARGIN + needed) {
        cs.close();
        newPage();
        drawTableHeader();
      }
    }

    private void drawHeader() throws IOException {
      text(FONT_BOLD, 14, MARGIN, y, companyName, null);
      String geradoEm =
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss"));
      textRightAligned(FONT, 9, pageWidth - MARGIN, y, "Gerado em: " + geradoEm, null);
      y -= 22;

      cs.setStrokingColor(0.7f, 0.7f, 0.7f);
      cs.setLineWidth(0.5f);
      cs.moveTo(MARGIN, y);
      cs.lineTo(pageWidth - MARGIN, y);
      cs.stroke();
      y -= 18;

      text(FONT_BOLD, 12, MARGIN, y, "RELATÓRIO DE TRANSAÇÕES", null);
      y -= 16;

      text(
          FONT,
          9,
          MARGIN,
          y,
          "Período: " + dataInicio.format(DATA_CURTA) + " a " + dataFim.format(DATA_CURTA),
          null);
      y -= 22;
    }

    private void drawTableHeader() throws IOException {
      cs.setNonStrokingColor(0.85f, 0.85f, 0.85f);
      cs.addRect(MARGIN, y - 4, pageWidth - 2 * MARGIN, 16);
      cs.fill();

      text(FONT_BOLD, 9, MARGIN + COL_DATA_X + 2, y, "Data", null);
      text(FONT_BOLD, 9, MARGIN + COL_BANCO_X + 2, y, "Banco", null);
      text(FONT_BOLD, 9, MARGIN + COL_CATEGORIA_X + 2, y, "Categoria", null);
      text(FONT_BOLD, 9, MARGIN + COL_HIST_X + 2, y, "Descrição", null);
      textRightAligned(FONT_BOLD, 9, pageWidth - MARGIN - 2, y, "Valor (R$)", null);
      y -= 16;
    }

    private void drawLinha(Transaction transacao) throws IOException {
      float size = 8;

      text(
          FONT,
          size,
          MARGIN + COL_DATA_X,
          y,
          transacao.getTransactionDate().format(DATA_CURTA),
          null);
      text(FONT, size, MARGIN + COL_BANCO_X, y, bankLabel(transacao), null);
      text(
          FONT,
          size,
          MARGIN + COL_CATEGORIA_X,
          y,
          truncate(TransactionUtil.categoryLabel(transacao.getCategory()), 18),
          null);

      float histRightEdge = pageWidth - MARGIN - COL_VALOR_WIDTH;
      float histWidth = histRightEdge - (MARGIN + COL_HIST_X);
      int maxChars = Math.max(10, (int) (histWidth / 4.2f));
      text(FONT, size, MARGIN + COL_HIST_X, y, truncate(transacao.getHistory(), maxChars), null);

      BigDecimal valor = transacao.getAmount();
      float[] cor = valor.signum() < 0 ? new float[] {0.75f, 0f, 0f} : new float[] {0f, 0.45f, 0f};
      textRightAligned(FONT, size, pageWidth - MARGIN - 2, y, formatValor(valor), cor);

      y -= 13;
    }

    private void drawResumo(int quantidade, BigDecimal totalEntradas, BigDecimal totalSaidas)
        throws IOException {
      y -= 8;
      cs.setStrokingColor(0.7f, 0.7f, 0.7f);
      cs.moveTo(MARGIN, y);
      cs.lineTo(pageWidth - MARGIN, y);
      cs.stroke();
      y -= 18;

      text(FONT_BOLD, 11, MARGIN, y, "RESUMO", null);
      y -= 16;

      text(FONT, 9, MARGIN, y, "Quantidade de transações: " + quantidade, null);
      y -= 14;
      text(FONT, 9, MARGIN, y, "Total de entradas:", null);
      textRightAligned(
          FONT,
          9,
          pageWidth - MARGIN - 2,
          y,
          formatValor(totalEntradas),
          new float[] {0f, 0.45f, 0f});
      y -= 14;
      text(FONT, 9, MARGIN, y, "Total de saídas:", null);
      textRightAligned(
          FONT,
          9,
          pageWidth - MARGIN - 2,
          y,
          formatValor(totalSaidas),
          new float[] {0.75f, 0f, 0f});
      y -= 14;
      text(FONT_BOLD, 9, MARGIN, y, "Saldo do período:", null);
      textRightAligned(
          FONT_BOLD,
          9,
          pageWidth - MARGIN - 2,
          y,
          formatValor(totalEntradas.add(totalSaidas)),
          null);
    }

    private String bankLabel(Transaction transacao) {
      if (transacao.getStatement() == null || transacao.getStatement().getBank() == null) {
        return "-";
      }
      Bank bank = transacao.getStatement().getBank();
      return switch (bank) {
        case BANCO_DO_BRASIL -> "BB";
        case SICOOB -> "Sicoob";
        case UNKNOWN -> "-";
      };
    }

    private String formatValor(BigDecimal valor) {
      return (valor.signum() < 0 ? "-" : "") + SicoobExtratoFormatUtil.formatValorAbsoluto(valor);
    }

    private void text(PDFont font, float size, float x, float yPos, String value, float[] rgb)
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

    private void textRightAligned(
        PDFont font, float size, float rightEdge, float yPos, String value, float[] rgb)
        throws IOException {
      float width = font.getStringWidth(value) / 1000 * size;
      text(font, size, rightEdge - width, yPos, value, rgb);
    }

    private String truncate(String value, int maxLength) {
      if (value == null) return "";
      return value.length() > maxLength ? value.substring(0, maxLength - 3) + "..." : value;
    }
  }
}
