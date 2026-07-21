package com.hortifruti.sl.hortifruti.service.finance;

import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobExtratoLinha;
import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobExtratoResponse;
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
 * Gera um Excel (.xlsx) de extrato a partir dos dados retornados pela API de conta corrente do
 * Sicoob, no mesmo layout do extrato original (título, cabeçalho Data/Documento/Histórico/Valor,
 * sub-linhas de complemento e linhas de SALDO DO DIA/SALDO ANTERIOR).
 */
@Component
@RequiredArgsConstructor
public class SicoobExtratoExcelGenerator {

  private final SicoobExtratoLayoutService layoutService;

  public byte[] generate(SicoobExtratoResponse extrato) throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Extrato");
      sheet.setColumnWidth(0, 256 * 12);
      sheet.setColumnWidth(1, 256 * 14);
      sheet.setColumnWidth(2, 256 * 42);
      sheet.setColumnWidth(3, 256 * 24);

      CellStyle titleStyle = titleStyle(workbook);
      CellStyle headerStyle = headerStyle(workbook);
      CellStyle normal = dataStyle(workbook, false, false);
      CellStyle bold = dataStyle(workbook, true, false);
      CellStyle normalRed = dataStyle(workbook, false, true);
      CellStyle boldRed = dataStyle(workbook, true, true);

      int rowIdx = 0;
      Row titleRow = sheet.createRow(rowIdx++);
      for (int c = 0; c < 4; c++) {
        Cell cell = titleRow.createCell(c);
        cell.setCellStyle(titleStyle);
      }
      titleRow.getCell(0).setCellValue("EXTRATO CONTA CORRENTE");
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

      Row headerRow = sheet.createRow(rowIdx++);
      String[] headers = {"DATA", "DOCUMENTO", "HISTÓRICO", "VALOR"};
      for (int c = 0; c < headers.length; c++) {
        Cell cell = headerRow.createCell(c);
        cell.setCellValue(headers[c]);
        cell.setCellStyle(headerStyle);
      }

      List<SicoobExtratoLinha> linhas = layoutService.montarLinhas(extrato);
      for (SicoobExtratoLinha linha : linhas) {
        Row row = sheet.createRow(rowIdx++);
        boolean debito = linha.valor() != null && linha.valor().signum() < 0;
        CellStyle style =
            linha.destaque() ? (debito ? boldRed : bold) : (debito ? normalRed : normal);

        setCell(
            row,
            0,
            linha.data() != null ? linha.data().format(SicoobExtratoFormatUtil.DATA_LONGA) : "",
            style);
        setCell(row, 1, linha.documento() != null ? linha.documento() : "", style);
        setCell(row, 2, linha.historico(), style);
        setCell(
            row,
            3,
            linha.valor() != null ? SicoobExtratoFormatUtil.formatValorComSinal(linha.valor()) : "",
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
