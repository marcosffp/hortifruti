package com.hortifruti.sl.hortifruti.service.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.hortifruti.sl.hortifruti.dto.bb.BBExtratoLinha;
import com.hortifruti.sl.hortifruti.util.SicoobExtratoFormatUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
        setCell(
            row,
            5,
            linha.valor() != null ? SicoobExtratoFormatUtil.formatValorComSinal(linha.valor()) : "",
            style);
        setCell(
            row,
            6,
            linha.saldo() != null ? SicoobExtratoFormatUtil.formatValorComSinal(linha.saldo()) : "",
            style);
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
}
