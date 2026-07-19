package com.hortifruti.sl.hortifruti.service.notification;

import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.service.finance.StatementService;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileGenerationService {

  private final StatementSelectionService statementSelectionService;
  private final StatementService statementService;

  public byte[] createZipWithStatements(int month, int year) throws IOException {
    List<Statement> statements = statementSelectionService.getBestStatementsForMonth(month, year);

    String tempZipPath =
        System.getProperty("java.io.tmpdir") + "/statements_" + month + "_" + year + ".zip";

    try (ZipFile zipFile = new ZipFile(tempZipPath)) {
      ZipParameters zipParameters = new ZipParameters();

      for (Statement statement : statements) {
        byte[] fileContent = statementService.getFileContent(statement);
        if (fileContent != null) {
          String fileName = statement.getName() + "_" + statement.getBank().name() + ".pdf";

          String tempPdfPath = System.getProperty("java.io.tmpdir") + "/" + fileName;
          try (FileOutputStream fos = new FileOutputStream(tempPdfPath)) {
            fos.write(fileContent);
          }

          zipParameters.setFileNameInZip(fileName);
          zipFile.addFile(tempPdfPath, zipParameters);

          new File(tempPdfPath).delete();
        }
      }

      List<Statement> bbStatements =
          statements.stream()
              .filter(s -> s.getBank() == Bank.BANCO_DO_BRASIL)
              .collect(Collectors.toList());
      if (!bbStatements.isEmpty()) {
        byte[] bbExcel = generateBankExcel(bbStatements, "Banco do Brasil");
        String bbExcelPath =
            System.getProperty("java.io.tmpdir") + "/extrato_bb_" + month + "_" + year + ".xlsx";
        try (FileOutputStream fos = new FileOutputStream(bbExcelPath)) {
          fos.write(bbExcel);
        }
        zipParameters.setFileNameInZip("extrato_bb_" + month + "_" + year + ".xlsx");
        zipFile.addFile(bbExcelPath, zipParameters);
        new File(bbExcelPath).delete();
      }

      byte[] invoicesZip = generateMonthlyInvoicesZip(month, year);
      if (invoicesZip.length > 0) {
        String invoicesZipPath =
            System.getProperty("java.io.tmpdir") + "/notas_fiscais_" + month + "_" + year + ".zip";
        try (FileOutputStream fos = new FileOutputStream(invoicesZipPath)) {
          fos.write(invoicesZip);
        }
        zipParameters.setFileNameInZip("notas_fiscais_" + month + "_" + year + ".zip");
        zipFile.addFile(invoicesZipPath, zipParameters);
        new File(invoicesZipPath).delete();
      }
    }

    byte[] zipBytes = java.nio.file.Files.readAllBytes(new File(tempZipPath).toPath());

    new File(tempZipPath).delete();

    return zipBytes;
  }

  public byte[] generateBankExcel(List<Statement> statements, String bankName) throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Extrato " + bankName);

      Row headerRow = sheet.createRow(0);
      String[] headers = {"Data", "Histórico", "Documento", "Valor", "Tipo", "Saldo"};

      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);

      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      int rowNum = 1;
      BigDecimal runningBalance = BigDecimal.ZERO;

      for (Statement statement : statements) {
        for (Transaction transaction : statement.getTransactions()) {
          Row row = sheet.createRow(rowNum++);

          row.createCell(0).setCellValue(transaction.getTransactionDate().toString());
          row.createCell(1).setCellValue(transaction.getHistory());
          row.createCell(2)
              .setCellValue(transaction.getDocument() != null ? transaction.getDocument() : "");
          row.createCell(3).setCellValue(transaction.getAmount().doubleValue());
          row.createCell(4).setCellValue(transaction.getTransactionType().toString());

          if (transaction.getTransactionType().toString().equals("CREDIT")) {
            runningBalance = runningBalance.add(transaction.getAmount());
          } else {
            runningBalance = runningBalance.subtract(transaction.getAmount());
          }
          row.createCell(5).setCellValue(runningBalance.doubleValue());
        }
      }

      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        workbook.write(outputStream);
        return outputStream.toByteArray();
      }
    }
  }

  public byte[] processGenericFiles(
      List<byte[]> files,
      List<String> fileNames,
      String debitValue,
      String creditValue,
      String cashValue)
      throws IOException {
    BigDecimal debit = parseValue(debitValue);
    BigDecimal credit = parseValue(creditValue);
    BigDecimal cash = parseValue(cashValue);

    BigDecimal processedDebit = debit.multiply(BigDecimal.valueOf(0.4));
    BigDecimal processedCredit = credit.multiply(BigDecimal.valueOf(0.4));

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Resumo Financeiro");

      Row headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("Tipo");
      headerRow.createCell(1).setCellValue("Valor Original");
      headerRow.createCell(2).setCellValue("Valor Processado");
      headerRow.createCell(3).setCellValue("Observação");

      Row debitRow = sheet.createRow(1);
      debitRow.createCell(0).setCellValue("Débito");
      debitRow.createCell(1).setCellValue(debit.doubleValue());
      debitRow.createCell(2).setCellValue(processedDebit.doubleValue());
      debitRow.createCell(3).setCellValue("60% removido");

      Row creditRow = sheet.createRow(2);
      creditRow.createCell(0).setCellValue("Crédito");
      creditRow.createCell(1).setCellValue(credit.doubleValue());
      creditRow.createCell(2).setCellValue(processedCredit.doubleValue());
      creditRow.createCell(3).setCellValue("60% removido");

      Row cashRow = sheet.createRow(3);
      cashRow.createCell(0).setCellValue("Dinheiro");
      cashRow.createCell(1).setCellValue(cash.doubleValue());
      cashRow.createCell(2).setCellValue(cash.doubleValue());
      cashRow.createCell(3).setCellValue("Sem alteração");

      for (int i = 0; i < 4; i++) {
        sheet.autoSizeColumn(i);
      }

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        workbook.write(outputStream);
        return outputStream.toByteArray();
      }
    }
  }

  private BigDecimal parseValue(String value) {
    if (value == null || value.trim().isEmpty()) {
      return BigDecimal.ZERO;
    }
    try {
      String cleanValue = value.replaceAll("[^0-9.,]", "");
      cleanValue = cleanValue.replace(",", ".");
      return new BigDecimal(cleanValue);
    } catch (Exception e) {
      return BigDecimal.ZERO;
    }
  }

  public byte[] generateClientBoleto(Long clientId) throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Boleto - Cliente " + clientId);

      Row headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("BOLETO BANCÁRIO");

      Row clientRow = sheet.createRow(1);
      clientRow.createCell(0).setCellValue("Cliente ID:");
      clientRow.createCell(1).setCellValue(clientId);

      Row dateRow = sheet.createRow(2);
      dateRow.createCell(0).setCellValue("Data de Emissão:");
      dateRow.createCell(1).setCellValue(java.time.LocalDate.now().toString());

      Row dueRow = sheet.createRow(3);
      dueRow.createCell(0).setCellValue("Data de Vencimento:");
      dueRow.createCell(1).setCellValue(java.time.LocalDate.now().plusDays(30).toString());

      Row valueRow = sheet.createRow(4);
      valueRow.createCell(0).setCellValue("Valor:");
      valueRow.createCell(1).setCellValue("R$ 0,00");

      for (int i = 0; i < 2; i++) {
        sheet.autoSizeColumn(i);
      }

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        workbook.write(outputStream);
        return outputStream.toByteArray();
      }
    }
  }

  public byte[] generateClientNotaFiscal(Long clientId) throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Nota Fiscal - Cliente " + clientId);

      Row headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("NOTA FISCAL");

      Row clientRow = sheet.createRow(1);
      clientRow.createCell(0).setCellValue("Cliente ID:");
      clientRow.createCell(1).setCellValue(clientId);

      Row dateRow = sheet.createRow(2);
      dateRow.createCell(0).setCellValue("Data de Emissão:");
      dateRow.createCell(1).setCellValue(java.time.LocalDate.now().toString());

      Row numberRow = sheet.createRow(3);
      numberRow.createCell(0).setCellValue("Número:");
      numberRow.createCell(1).setCellValue("NF-" + System.currentTimeMillis());

      Row valueRow = sheet.createRow(4);
      valueRow.createCell(0).setCellValue("Valor Total:");
      valueRow.createCell(1).setCellValue("R$ 0,00");

      for (int i = 0; i < 2; i++) {
        sheet.autoSizeColumn(i);
      }

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        workbook.write(outputStream);
        return outputStream.toByteArray();
      }
    }
  }

  public byte[] generateMonthlyInvoicesZip(int month, int year) throws IOException {
    try {
      String tempZipPath =
          System.getProperty("java.io.tmpdir") + "/invoices_" + month + "_" + year + ".zip";

      try (ZipFile zipFile = new ZipFile(tempZipPath)) {
        ZipParameters zipParameters = new ZipParameters();

        for (int i = 1; i <= 5; i++) {
          byte[] invoice = generateSampleInvoice(i, month, year);
          String invoicePath =
              System.getProperty("java.io.tmpdir")
                  + "/nota_fiscal_"
                  + i
                  + "_"
                  + month
                  + "_"
                  + year
                  + ".xlsx";

          try (FileOutputStream fos = new FileOutputStream(invoicePath)) {
            fos.write(invoice);
          }

          zipParameters.setFileNameInZip("nota_fiscal_" + i + "_" + month + "_" + year + ".xlsx");
          zipFile.addFile(invoicePath, zipParameters);

          new File(invoicePath).delete();
        }
      }

      byte[] zipBytes = java.nio.file.Files.readAllBytes(new File(tempZipPath).toPath());

      new File(tempZipPath).delete();

      return zipBytes;

    } catch (Exception e) {
      return new byte[0];
    }
  }

  private byte[] generateSampleInvoice(int invoiceNumber, int month, int year) throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Nota Fiscal " + invoiceNumber);

      Row headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("NOTA FISCAL ELETRÔNICA");

      Row numberRow = sheet.createRow(1);
      numberRow.createCell(0).setCellValue("Número:");
      numberRow
          .createCell(1)
          .setCellValue("NF-" + year + month + String.format("%03d", invoiceNumber));

      Row dateRow = sheet.createRow(2);
      dateRow.createCell(0).setCellValue("Data de Emissão:");
      dateRow.createCell(1).setCellValue(String.format("%02d/%02d/%d", 15, month, year));

      Row clientRow = sheet.createRow(3);
      clientRow.createCell(0).setCellValue("Cliente:");
      clientRow.createCell(1).setCellValue("Cliente " + invoiceNumber);

      Row valueRow = sheet.createRow(4);
      valueRow.createCell(0).setCellValue("Valor Total:");
      valueRow.createCell(1).setCellValue("R$ " + (invoiceNumber * 150.50));

      for (int i = 0; i < 2; i++) {
        sheet.autoSizeColumn(i);
      }

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        workbook.write(outputStream);
        return outputStream.toByteArray();
      }
    }
  }
}
