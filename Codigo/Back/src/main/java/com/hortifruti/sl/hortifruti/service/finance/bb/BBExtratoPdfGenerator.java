package com.hortifruti.sl.hortifruti.service.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.dto.bb.BBExtratoLinha;
import com.hortifruti.sl.hortifruti.util.SicoobExtratoFormatUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Gera um PDF de extrato a partir dos lançamentos retornados pela API Extratos v2 do BB — caminho
 * inverso do fluxo de upload manual (que extrai texto de um PDF já pronto), no mesmo layout visual
 * do extrato original do BB (cabeçalho, tabela Data/Ag.Origem/Lote/Documento/Histórico/Valor/Saldo
 * com sub-linha de complemento), ver {@code 00914062026 (1).pdf} usado como referência.
 */
@Component
@RequiredArgsConstructor
public class BBExtratoPdfGenerator {

  private final BBExtratoLayoutService layoutService;

  @Value("${company.name}")
  private String companyName;

  private static final float COL_DATA_X = 0;
  private static final float COL_AG_X = 46;
  private static final float COL_LOTE_X = 78;
  private static final float COL_DOC_X = 112;
  private static final float COL_HIST_X = 185;
  private static final float COL_VALOR_WIDTH = 75;

  public byte[] generate(
      LocalDate dataInicio,
      LocalDate dataFim,
      String agencia,
      String conta,
      List<JsonNode> lancamentos)
      throws IOException {
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream()) {

      List<BBExtratoLinha> linhas = layoutService.montarLinhas(lancamentos);
      new PageWriter(document, dataInicio, dataFim, agencia, conta).write(linhas);

      document.save(pdfOut);
      return pdfOut.toByteArray();
    }
  }

  /**
   * Estado de escrita de uma execução (página atual, posição Y) — evita campos de instância
   * mutáveis compartilhados entre chamadas concorrentes ao gerador.
   */
  private class PageWriter extends AbstractPdfPageWriter {
    private final LocalDate dataInicio;
    private final LocalDate dataFim;
    private final String agencia;
    private final String conta;

    PageWriter(
        PDDocument document,
        LocalDate dataInicio,
        LocalDate dataFim,
        String agencia,
        String conta) {
      super(document);
      this.dataInicio = dataInicio;
      this.dataFim = dataFim;
      this.agencia = agencia;
      this.conta = conta;
    }

    void write(List<BBExtratoLinha> linhas) throws IOException {
      newPage();
      drawHeader();
      drawTableHeader();

      for (BBExtratoLinha linha : linhas) {
        ensureSpace();
        drawLinha(linha);
      }

      cs.close();
    }

    private void drawHeader() throws IOException {
      text(FONT_BOLD, 18, MARGIN, y, "BANCO DO BRASIL", new float[] {0.96f, 0.78f, 0.0f});
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

      text(
          FONT,
          9,
          MARGIN,
          y,
          "Agência: " + agencia + " / Conta: " + conta + " / " + companyName,
          null);
      y -= 14;

      text(
          FONT,
          9,
          MARGIN,
          y,
          "Período: "
              + dataInicio.format(SicoobExtratoFormatUtil.DATA_LONGA)
              + " - "
              + dataFim.format(SicoobExtratoFormatUtil.DATA_LONGA),
          null);
      y -= 22;

      text(FONT_BOLD, 11, MARGIN, y, "LANÇAMENTOS", null);
      y -= 18;
    }

    @Override
    protected void drawTableHeader() throws IOException {
      cs.setNonStrokingColor(0.85f, 0.85f, 0.85f);
      cs.addRect(MARGIN, y - 4, pageWidth - 2 * MARGIN, 16);
      cs.fill();

      text(FONT_BOLD, 8, MARGIN + COL_DATA_X + 2, y, "Data", null);
      text(FONT_BOLD, 8, MARGIN + COL_AG_X + 2, y, "Ag.Orig", null);
      text(FONT_BOLD, 8, MARGIN + COL_LOTE_X + 2, y, "Lote", null);
      text(FONT_BOLD, 8, MARGIN + COL_DOC_X + 2, y, "Documento", null);
      text(FONT_BOLD, 8, MARGIN + COL_HIST_X + 2, y, "Histórico", null);
      textRightAligned(FONT_BOLD, 8, pageWidth - MARGIN - COL_VALOR_WIDTH, y, "Valor R$");
      textRightAligned(FONT_BOLD, 8, pageWidth - MARGIN - 2, y, "Saldo");
      y -= 16;
    }

    private void drawLinha(BBExtratoLinha linha) throws IOException {
      PDFont font = linha.destaque() ? FONT_BOLD : FONT;
      float size = 7.5f;
      boolean isSubLinha = linha.dataBalancete() == null;

      if (!isSubLinha) {
        text(
            font,
            size,
            MARGIN + COL_DATA_X,
            y,
            linha.dataBalancete().format(SicoobExtratoFormatUtil.DATA_CURTA),
            null);
        if (linha.agenciaOrigem() != null) {
          text(font, size, MARGIN + COL_AG_X, y, linha.agenciaOrigem(), null);
        }
        if (linha.lote() != null) {
          text(font, size, MARGIN + COL_LOTE_X, y, linha.lote(), null);
        }
        if (linha.documento() != null) {
          text(font, size, MARGIN + COL_DOC_X, y, truncate(linha.documento(), 16), null);
        }
      }

      float histX = MARGIN + COL_HIST_X + (isSubLinha ? 8 : 0);
      text(font, size, histX, y, truncate(linha.historico(), 48), null);

      if (linha.valor() != null) {
        float[] cor =
            linha.valor().signum() < 0 ? new float[] {0.75f, 0f, 0f} : new float[] {0f, 0f, 0f};
        textRightAligned(
            font,
            size,
            pageWidth - MARGIN - COL_VALOR_WIDTH,
            y,
            SicoobExtratoFormatUtil.formatValorComSinal(linha.valor()),
            cor);
      }
      if (linha.saldo() != null) {
        textRightAligned(
            font,
            size,
            pageWidth - MARGIN - 2,
            y,
            SicoobExtratoFormatUtil.formatValorComSinal(linha.saldo()));
      }

      y -= 12;
    }
  }
}
