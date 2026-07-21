package com.hortifruti.sl.hortifruti.service.finance;

import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobExtratoLinha;
import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobExtratoResponse;
import com.hortifruti.sl.hortifruti.util.SicoobExtratoFormatUtil;
import com.hortifruti.sl.hortifruti.util.SicoobExtratoParsingUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
 * Gera um PDF de extrato a partir dos dados retornados pela API de conta corrente do Sicoob —
 * caminho inverso do {@code PdfUtil.extractPdfText}: em vez de extrair texto de um PDF já pronto,
 * desenha um PDF a partir de dados estruturados, no mesmo layout do extrato original do Sicoob
 * (cabeçalho, tabela Data/Documento/Histórico/Valor com sub-linhas e SALDO DO DIA, resumo).
 */
@Component
@RequiredArgsConstructor
public class SicoobExtratoPdfGenerator {

  private final SicoobExtratoLayoutService layoutService;

  @Value("${company.name}")
  private String companyName;

  private static final float MARGIN = 40;
  private static final float COL_DATA_X = 0;
  private static final float COL_DOC_X = 55;
  private static final float COL_HIST_X = 130;
  private static final PDFont FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

  public byte[] generate(
      LocalDate periodoInicio,
      LocalDate periodoFim,
      long numeroContaCorrente,
      SicoobExtratoResponse extrato)
      throws IOException {
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream()) {

      List<SicoobExtratoLinha> linhas = layoutService.montarLinhas(extrato);
      new PageWriter(document, periodoInicio, periodoFim, numeroContaCorrente, extrato)
          .write(linhas);

      document.save(pdfOut);
      return pdfOut.toByteArray();
    }
  }

  /**
   * Estado de escrita de uma execução (página atual, posição Y) — evitando campos de instância
   * mutáveis compartilhados entre chamadas concorrentes ao gerador.
   */
  private class PageWriter {
    private final PDDocument document;
    private final LocalDate periodoInicio;
    private final LocalDate periodoFim;
    private final long numeroContaCorrente;
    private final SicoobExtratoResponse extrato;
    private PDPage page;
    private PDPageContentStream cs;
    private float y;
    private float pageWidth;
    private float pageHeight;

    PageWriter(
        PDDocument document,
        LocalDate periodoInicio,
        LocalDate periodoFim,
        long numeroContaCorrente,
        SicoobExtratoResponse extrato) {
      this.document = document;
      this.periodoInicio = periodoInicio;
      this.periodoFim = periodoFim;
      this.numeroContaCorrente = numeroContaCorrente;
      this.extrato = extrato;
    }

    void write(List<SicoobExtratoLinha> linhas) throws IOException {
      newPage();
      drawHeader();
      drawTableHeader();

      for (SicoobExtratoLinha linha : linhas) {
        ensureSpace();
        drawLinha(linha);
      }

      ensureSpace(80);
      drawResumo();
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
      text(FONT_BOLD, 18, MARGIN, y, "SICOOB", new float[] {0.0f, 0.5f, 0.2f});
      text(FONT_BOLD, 10, MARGIN + 90, y + 5, "SISTEMA DE COOPERATIVAS DE CRÉDITO DO BRASIL", null);
      text(
          FONT,
          8,
          MARGIN + 90,
          y - 6,
          "PLATAFORMA DE SERVIÇOS FINANCEIROS DO SICOOB - SISBR",
          null);
      y -= 30;

      cs.setStrokingColor(0.7f, 0.7f, 0.7f);
      cs.setLineWidth(0.5f);
      cs.moveTo(MARGIN, y);
      cs.lineTo(pageWidth - MARGIN, y);
      cs.stroke();
      y -= 18;

      String geradoEm =
          LocalDateTime.now()
              .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss"));
      text(FONT_BOLD, 12, MARGIN, y, "EXTRATO DE CONTA CORRENTE", null);
      textRightAligned(FONT, 9, pageWidth - MARGIN, y, geradoEm);
      y -= 16;

      text(FONT, 9, MARGIN, y, "Conta: " + numeroContaCorrente + " / " + companyName, null);
      y -= 14;

      text(
          FONT,
          9,
          MARGIN,
          y,
          "Período: "
              + periodoInicio.format(SicoobExtratoFormatUtil.DATA_LONGA)
              + " - "
              + periodoFim.format(SicoobExtratoFormatUtil.DATA_LONGA),
          null);
      y -= 22;

      text(FONT_BOLD, 11, MARGIN, y, "HISTÓRICO DE MOVIMENTAÇÃO", null);
      y -= 18;
    }

    private void drawTableHeader() throws IOException {
      cs.setNonStrokingColor(0.85f, 0.85f, 0.85f);
      cs.addRect(MARGIN, y - 4, pageWidth - 2 * MARGIN, 16);
      cs.fill();

      text(FONT_BOLD, 9, MARGIN + COL_DATA_X + 2, y, "Data", null);
      text(FONT_BOLD, 9, MARGIN + COL_DOC_X + 2, y, "Documento", null);
      text(FONT_BOLD, 9, MARGIN + COL_HIST_X + 2, y, "Histórico", null);
      textRightAligned(FONT_BOLD, 9, pageWidth - MARGIN - 2, y, "Valor");
      y -= 16;
    }

    private void drawLinha(SicoobExtratoLinha linha) throws IOException {
      PDFont font = linha.destaque() ? FONT_BOLD : FONT;
      float size = 8;

      if (linha.data() != null) {
        text(
            font,
            size,
            MARGIN + COL_DATA_X,
            y,
            linha.data().format(SicoobExtratoFormatUtil.DATA_CURTA),
            null);
      }
      if (linha.documento() != null) {
        text(font, size, MARGIN + COL_DOC_X, y, truncate(linha.documento(), 12), null);
      }

      float histX =
          MARGIN + COL_HIST_X + (linha.data() == null && linha.documento() == null ? 6 : 0);
      text(font, size, histX, y, truncate(linha.historico(), 55), null);

      if (linha.valor() != null) {
        float[] cor =
            linha.valor().signum() < 0 ? new float[] {0.75f, 0f, 0f} : new float[] {0f, 0f, 0f};
        textRightAligned(
            font,
            size,
            pageWidth - MARGIN - 2,
            y,
            SicoobExtratoFormatUtil.formatValorComSinal(linha.valor()),
            cor);
      }

      y -= 13;
    }

    private void drawResumo() throws IOException {
      y -= 8;
      cs.setStrokingColor(0.7f, 0.7f, 0.7f);
      cs.moveTo(MARGIN, y);
      cs.lineTo(pageWidth - MARGIN, y);
      cs.stroke();
      y -= 18;

      text(FONT_BOLD, 11, MARGIN, y, "RESUMO", null);
      y -= 16;

      resumoLinha("Saldo anterior", extrato.saldoAnterior());
      resumoLinha("Saldo atual", extrato.saldoAtual());
      resumoLinha("Saldo bloqueado", extrato.saldoBloqueado());
      resumoLinha("Limite disponível", extrato.saldoLimite());
      resumoLinha("Saldo bloqueio judicial", extrato.saldoBloqueioJudicial());
      resumoLinha("Saldo bloqueio judicial anterior", extrato.saldoBloqueioJudicialAnterior());
    }

    private void resumoLinha(String rotulo, String valorBruto) throws IOException {
      if (valorBruto == null) {
        return;
      }
      BigDecimal valor = SicoobExtratoParsingUtil.parseSaldo(valorBruto);
      text(FONT, 9, MARGIN, y, rotulo + ":", null);
      textRightAligned(
          FONT, 9, pageWidth - MARGIN - 2, y, SicoobExtratoFormatUtil.formatValorComSinal(valor));
      y -= 14;
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
        PDFont font, float size, float rightEdge, float yPos, String value) throws IOException {
      textRightAligned(font, size, rightEdge, yPos, value, null);
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
