package com.hortifruti.sl.hortifruti.service.transaction;

import com.hortifruti.sl.hortifruti.model.Transaction;
import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.repository.TransactionRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionExcelExportService {

  private final TransactionRepository transactionRepository;

  public Map<String, byte[]> exportTransactionsAsExcel() throws IOException {
    LocalDate now = LocalDate.now();
    LocalDate firstDayLastMonth = now.minusMonths(1).withDayOfMonth(1);
    LocalDate lastDayLastMonth = now.withDayOfMonth(1).minusDays(1);

    List<Transaction> transactions =
        transactionRepository.findByTransactionDateBetweenAndStatementBank(
            firstDayLastMonth, lastDayLastMonth, Bank.BANCO_DO_BRASIL);

    Map<String, byte[]> excelData = new HashMap<>();
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream excelOut = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Transactions");

      createHeaderRow(sheet, workbook);
      populateDataRows(sheet, workbook, transactions);
      adjustColumnWidths(sheet);

      workbook.write(excelOut);
      byte[] excelBytes = excelOut.toByteArray();

      // Nome do arquivo Excel
      String currentMonth =
          LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
      String excelFileName = "Planilha-Hortifruti-Santa-Luzia-" + currentMonth + ".xlsx";

      // Configurar o cabeçalho da resposta
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + excelFileName);
      excelData.put(excelFileName, excelBytes);
    }
    return excelData;
  }

  private void createHeaderRow(Sheet sheet, Workbook workbook) {
    Row headerRow = sheet.createRow(0);
    String[] columnHeaders = {
      "Data",
      "Documento",
      "Cod.Histórico",
      "Histórico",
      "Agência de Origem",
      "Lote",
      "R$ Valor",
      "Info.",
      "Complemento"
    };

    for (int i = 0; i < columnHeaders.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(columnHeaders[i]);
      cell.setCellStyle(createHeaderCellStyle(workbook));
    }
  }

  private void populateDataRows(Sheet sheet, Workbook workbook, List<Transaction> transactions) {
    int rowIdx = 1;
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    for (Transaction transaction : transactions) {
      Row row = sheet.createRow(rowIdx++);
      populateRowData(row, workbook, transaction, dateFormatter);
    }
  }

  private void populateRowData(
      Row row, Workbook workbook, Transaction transaction, DateTimeFormatter dateFormatter) {
    CellStyle dataCellStyle = createDataCellStyle(workbook);

    createAndStyleCell(
        row, 0, transaction.getTransactionDate().format(dateFormatter), dataCellStyle);
    createAndStyleCell(row, 1, transaction.getDocument(), dataCellStyle);
    createAndStyleCell(row, 2, transaction.getCodHistory(), dataCellStyle);
    createAndStyleCell(row, 3, transaction.getHistory(), dataCellStyle);
    createAndStyleCell(row, 4, transaction.getSourceAgency(), dataCellStyle);
    createAndStyleCell(row, 5, transaction.getBatch(), dataCellStyle);

    setAmountCell(row, 6, transaction, workbook);
    createAndStyleCell(row, 7, transaction.getTransactionType().toString(), dataCellStyle);
    createAndStyleCell(row, 8, determineComplement(transaction), dataCellStyle);
  }

  private void createAndStyleCell(Row row, int column, String value, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  private void setAmountCell(Row row, int column, Transaction transaction, Workbook workbook) {
    Cell cell = row.createCell(column);
    double amountValue = transaction.getAmount().doubleValue();

    if (amountValue < 0) {
      cell.setCellValue(-amountValue);
      cell.setCellStyle(createNegativeAmountCellStyle(workbook));
    } else {
      cell.setCellValue(amountValue);
      cell.setCellStyle(createDefaultAmountCellStyle(workbook));
    }
  }

  private String determineComplement(Transaction transaction) {
    if (transaction.getHistory() != null && transaction.getHistory().contains("Marlucia")) {
      return "Pagamento de fornecedor";
    }

    return switch (transaction.getCategory()) {
      case VENDAS_CARTAO -> "Antecipação dos Recebíveis";
      case VENDAS_PIX -> "Recebimento de vendas";
      case FORNECEDOR -> "Pagamento de fornecedor";
      case CEMIG, COPASA, FISCAL, SERVICOS_TELEFONICOS -> "Pagamento de serviço";
      case FUNCIONARIO -> "Pagamento de funcionário";
      case FAMÍLIA -> "Pagamento de serviços externos";
      case SERVICOS_BANCARIOS -> "Pagamento de serviços bancários";
      case IMPOSTOS -> "Pagamento de imposto";
      default -> transaction.getCategory().toString();
    };
  }

  private void adjustColumnWidths(Sheet sheet) {
    sheet.setColumnWidth(3, 256 * 70); // Ajusta "Histórico" para 70 caracteres
    sheet.setColumnWidth(6, 256 * 15); // Ajusta "R$ Valor" para 15 caracteres

    for (int i = 0; i < 9; i++) {
      if (i != 3 && i != 6) {
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
