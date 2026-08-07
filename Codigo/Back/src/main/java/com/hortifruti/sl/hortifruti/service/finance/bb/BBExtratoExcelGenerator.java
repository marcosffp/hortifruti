package com.hortifruti.sl.hortifruti.service.finance.bb;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.dto.bb.BBExtratoLinha;
import com.hortifruti.sl.hortifruti.util.SicoobExtratoFormatUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Gera um Excel (.xlsx) de extrato a partir dos lançamentos retornados pela API Extratos v2 do BB,
 * no mesmo layout do extrato original (título, cabeçalho Data/Ag.Origem/Lote/Documento/Histórico/
 * Valor/Saldo, sub-linhas de complemento).
 */
@Component
@RequiredArgsConstructor
public class BBExtratoExcelGenerator {

  /**
   * Mesmo texto exibido por {@link SicoobExtratoFormatUtil#formatValorComSinal}, só que como
   * formato de célula numérica (seção positiva/negativa do Excel) em vez de {@code String} —
   * assim a coluna VALOR/SALDO continua um número de verdade (soma/filtra no Excel), com o mesmo
   * "R$ "/" C"/" D" exibido antes.
   */
  private static final String VALOR_FORMAT = "\"R$ \"#,##0.00\" C\";\"R$ \"#,##0.00\" D\"";

  private final BBExtratoLayoutService layoutService;

  public byte[] generate(List<JsonNode> lancamentos) throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Extrato");
      sheet.setColumnWidth(0, 256 * 10);
      sheet.setColumnWidth(1, 256 * 9);
      sheet.setColumnWidth(2, 256 * 9);
      sheet.setColumnWidth(3, 256 * 16);
      sheet.setColumnWidth(4, 256 * 42);
      sheet.setColumnWidth(5, 256 * 16);
      sheet.setColumnWidth(6, 256 * 16);

      CellStyle titleStyle = titleStyle(workbook);
      CellStyle headerStyle = headerStyle(workbook);
      CellStyle normal = dataStyle(workbook, false, false);
      CellStyle bold = dataStyle(workbook, true, false);
      CellStyle normalRed = dataStyle(workbook, false, true);
      CellStyle boldRed = dataStyle(workbook, true, true);
      CellStyle normalAmount = amountStyle(workbook, false, false);
      CellStyle boldAmount = amountStyle(workbook, true, false);
      CellStyle normalRedAmount = amountStyle(workbook, false, true);
      CellStyle boldRedAmount = amountStyle(workbook, true, true);

      int rowIdx = 0;
      Row titleRow = sheet.createRow(rowIdx++);
      for (int c = 0; c < 7; c++) {
        Cell cell = titleRow.createCell(c);
        cell.setCellStyle(titleStyle);
      }
      titleRow.getCell(0).setCellValue("EXTRATO CONTA CORRENTE - BANCO DO BRASIL");
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

      Row headerRow = sheet.createRow(rowIdx++);
      String[] headers = {"DATA", "AG.ORIGEM", "LOTE", "DOCUMENTO", "HISTÓRICO", "VALOR", "SALDO"};
      for (int c = 0; c < headers.length; c++) {
        Cell cell = headerRow.createCell(c);
        cell.setCellValue(headers[c]);
        cell.setCellStyle(headerStyle);
      }

      List<BBExtratoLinha> linhas = layoutService.montarLinhas(lancamentos);
      for (BBExtratoLinha linha : linhas) {
        Row row = sheet.createRow(rowIdx++);
        boolean debito = linha.valor() != null && linha.valor().signum() < 0;
        CellStyle style =
            linha.destaque() ? (debito ? boldRed : bold) : (debito ? normalRed : normal);
        CellStyle amountCellStyle =
            linha.destaque()
                ? (debito ? boldRedAmount : boldAmount)
                : (debito ? normalRedAmount : normalAmount);

        setCell(
            row,
            0,
            linha.dataBalancete() != null
                ? linha.dataBalancete().format(SicoobExtratoFormatUtil.DATA_LONGA)
                : "",
            style);
        setCell(row, 1, linha.agenciaOrigem() != null ? linha.agenciaOrigem() : "", style);
        setCell(row, 2, linha.lote() != null ? linha.lote() : "", style);
        setCell(row, 3, linha.documento() != null ? linha.documento() : "", style);
        setCell(row, 4, linha.historico(), style);
        setAmountCell(row, 5, linha.valor(), amountCellStyle);
        setAmountCell(row, 6, linha.saldo(), amountCellStyle);
      }

      workbook.write(out);
      return out.toByteArray();
    }
  }

  private void setCell(Row row, int column, String value, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  private void setAmountCell(Row row, int column, BigDecimal valor, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellStyle(style);
    if (valor != null) {
      cell.setCellValue(valor.setScale(2, RoundingMode.HALF_UP).doubleValue());
    }
  }

  private CellStyle titleStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    font.setFontHeightInPoints((short) 14);
    style.setFont(font);
    return style;
  }

  private CellStyle headerStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
    return style;
  }

  private CellStyle dataStyle(Workbook workbook, boolean bold, boolean red) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(bold);
    if (red) {
      font.setColor(IndexedColors.RED.getIndex());
    }
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.LEFT);
    return style;
  }

  private CellStyle amountStyle(Workbook workbook, boolean bold, boolean red) {
    CellStyle style = dataStyle(workbook, bold, red);
    style.setDataFormat(workbook.createDataFormat().getFormat(VALOR_FORMAT));
    return style;
  }
}
