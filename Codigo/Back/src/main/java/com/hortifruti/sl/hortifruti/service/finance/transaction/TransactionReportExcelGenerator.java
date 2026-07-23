package com.hortifruti.sl.hortifruti.service.finance;

import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.util.TransactionUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Gera o relatório consolidado de transações (BB + Sicoob juntos, em ordem cronológica) em Excel.
 */
@Component
public class TransactionReportExcelGenerator {

  private static final DateTimeFormatter DATA_CURTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  public byte[] generate(List<Transaction> transacoes) throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream excelOut = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Relatório de Transações");

      createHeaderRow(sheet, workbook);
      populateDataRows(sheet, workbook, transacoes);
      adjustColumnWidths(sheet);

      workbook.write(excelOut);
      return excelOut.toByteArray();
    }
  }

  private void createHeaderRow(Sheet sheet, Workbook workbook) {
    Row headerRow = sheet.createRow(0);
    String[] columnHeaders = {"Data", "Banco", "Categoria", "Descrição", "R$ Valor"};

    for (int i = 0; i < columnHeaders.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(columnHeaders[i]);
      cell.setCellStyle(createHeaderCellStyle(workbook));
    }
  }

  private void populateDataRows(Sheet sheet, Workbook workbook, List<Transaction> transacoes) {
    int rowIdx = 1;
    for (Transaction transacao : transacoes) {
      Row row = sheet.createRow(rowIdx++);
      populateRowData(row, workbook, transacao);
    }
  }

  private void populateRowData(Row row, Workbook workbook, Transaction transacao) {
    CellStyle dataCellStyle = createDataCellStyle(workbook);

    createAndStyleCell(row, 0, transacao.getTransactionDate().format(DATA_CURTA), dataCellStyle);
    createAndStyleCell(row, 1, bankLabel(transacao), dataCellStyle);
    createAndStyleCell(
        row, 2, TransactionUtil.categoryLabel(transacao.getCategory()), dataCellStyle);
    createAndStyleCell(row, 3, transacao.getHistory(), dataCellStyle);
    setAmountCell(row, 4, transacao, workbook);
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

  private void createAndStyleCell(Row row, int column, String value, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  private void setAmountCell(Row row, int column, Transaction transacao, Workbook workbook) {
    Cell cell = row.createCell(column);
    double amountValue = transacao.getAmount().doubleValue();

    if (amountValue < 0) {
      cell.setCellValue(-amountValue);
      cell.setCellStyle(createNegativeAmountCellStyle(workbook));
    } else {
      cell.setCellValue(amountValue);
      cell.setCellStyle(createDefaultAmountCellStyle(workbook));
    }
  }

  private void adjustColumnWidths(Sheet sheet) {
    sheet.setColumnWidth(3, 256 * 60);
    sheet.setColumnWidth(4, 256 * 15);

    for (int i = 0; i < 5; i++) {
      if (i != 3 && i != 4) {
        sheet.autoSizeColumn(i);
      }
    }
  }

  private CellStyle createHeaderCellStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    font.setColor(IndexedColors.WHITE.getIndex());
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setFillForegroundColor(IndexedColors.BLACK.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    setBorders(style, IndexedColors.BLACK.getIndex());
    return style;
  }

  private CellStyle createDataCellStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    return style;
  }

  private CellStyle createNegativeAmountCellStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setColor(IndexedColors.RED.getIndex());
    style.setFont(font);
    style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    return style;
  }

  private CellStyle createDefaultAmountCellStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setColor(IndexedColors.BLACK.getIndex());
    style.setFont(font);
    style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    return style;
  }

  private void setBorders(CellStyle style, short color) {
    style.setBorderTop(BorderStyle.THICK);
    style.setBorderBottom(BorderStyle.THICK);
    style.setBorderLeft(BorderStyle.THICK);
    style.setBorderRight(BorderStyle.THICK);
    style.setTopBorderColor(color);
    style.setBottomBorderColor(color);
    style.setLeftBorderColor(color);
    style.setRightBorderColor(color);
  }
}
