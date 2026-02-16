package com.hortifruti.sl.hortifruti.service.finance;

import com.hortifruti.sl.hortifruti.model.enumeration.Bank;
import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.repository.finance.TransactionRepository;
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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionPdfExportService {

  private final TransactionRepository transactionRepository;

  public Map<String, byte[]> exportTransactionsAsPdf() throws IOException {
    LocalDate now = LocalDate.now();
    LocalDate firstDayLastMonth = now.minusMonths(1).withDayOfMonth(1);
    LocalDate lastDayLastMonth = now.withDayOfMonth(1).minusDays(1);

    List<Transaction> transactions =
        transactionRepository.findByTransactionDateBetweenAndStatementBank(
            firstDayLastMonth, lastDayLastMonth, Bank.BANCO_DO_BRASIL);

    Map<String, byte[]> pdfData = new HashMap<>();

    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream()) {

      createPdfFromTransactions(document, transactions);
      document.save(pdfOut);
      byte[] pdfBytes = pdfOut.toByteArray();

      String currentMonth =
          LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
      String pdfFileName = "HORTIFRUTI_SANTA_LUZIA-" + currentMonth + ".pdf";

      pdfData.put(pdfFileName, pdfBytes);
    }

    return pdfData;
  }

  private void createPdfFromTransactions(PDDocument document, List<Transaction> transactions)
      throws IOException {
    // Usar A4 em modo paisagem diretamente
    PDPage page =
        new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
    document.addPage(page);

    PDPageContentStream contentStream = new PDPageContentStream(document, page);
    try {
      // Configurações para modo paisagem - agora corretas
      float pageWidth = page.getMediaBox().getWidth(); // 842
      float pageHeight = page.getMediaBox().getHeight(); // 595
      float margin = 30;

      // Título
      contentStream.beginText();
      contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
      contentStream.newLineAtOffset(margin, pageHeight - 50);
      contentStream.showText("Relatório de Transações - Hortifruti Santa Luzia");
      contentStream.endText();

      // Cabeçalho da tabela
      float yPosition = pageHeight - 100;
      float[] columnWidths = {70, 90, 80, 150, 80, 50, 80, 60, 120};
      String[] headers = {
        "Data",
        "Documento",
        "Cod.Histórico",
        "Histórico",
        "Agência",
        "Lote",
        "R$ Valor",
        "Info.",
        "Complemento"
      };

      drawTableHeader(contentStream, margin, yPosition, columnWidths, headers, pageWidth);

      // Dados da tabela
      yPosition -= 25;
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

      for (Transaction transaction : transactions) {
        if (yPosition < margin + 50) {
          // Nova página se necessário
          contentStream.close();
          page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
          document.addPage(page);
          contentStream = new PDPageContentStream(document, page);
          yPosition = pageHeight - 50;
          drawTableHeader(contentStream, margin, yPosition, columnWidths, headers, pageWidth);
          yPosition -= 25;
        }

        drawTableRow(contentStream, margin, yPosition, columnWidths, transaction, dateFormatter);
        yPosition -= 20;
      }
    } finally {
      contentStream.close();
    }
  }

  private void drawTableHeader(
      PDPageContentStream contentStream,
      float margin,
      float yPosition,
      float[] columnWidths,
      String[] headers,
      float pageWidth)
      throws IOException {

    // Desenhar retângulo de fundo para o cabeçalho
    contentStream.setNonStrokingColor(0.8f, 0.8f, 0.8f);
    contentStream.addRect(margin, yPosition - 5, pageWidth - 2 * margin, 20);
    contentStream.fill();

    // Desenhar bordas
    contentStream.setStrokingColor(0, 0, 0);
    contentStream.setLineWidth(1);
    contentStream.addRect(margin, yPosition - 5, pageWidth - 2 * margin, 20);
    contentStream.stroke();

    // Texto do cabeçalho
    contentStream.setNonStrokingColor(0, 0, 0);
    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);

    float xPosition = margin + 5;
    for (int i = 0; i < headers.length; i++) {
      contentStream.beginText();
      contentStream.newLineAtOffset(xPosition, yPosition + 5);
      contentStream.showText(headers[i]);
      contentStream.endText();
      xPosition += columnWidths[i];
    }
  }

  private void drawTableRow(
      PDPageContentStream contentStream,
      float margin,
      float yPosition,
      float[] columnWidths,
      Transaction transaction,
      DateTimeFormatter dateFormatter)
      throws IOException {

    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
    contentStream.setNonStrokingColor(0, 0, 0);

    String[] rowData = {
      transaction.getTransactionDate().format(dateFormatter),
      truncateText(transaction.getDocument() != null ? transaction.getDocument() : "", 12),
      truncateText(transaction.getCodHistory() != null ? transaction.getCodHistory() : "", 10),
      truncateText(transaction.getHistory() != null ? transaction.getHistory() : "", 25),
      truncateText(transaction.getSourceAgency() != null ? transaction.getSourceAgency() : "", 10),
      truncateText(transaction.getBatch() != null ? transaction.getBatch() : "", 8),
      String.format("%.2f", Math.abs(transaction.getAmount().doubleValue())),
      truncateText(transaction.getTransactionType().toString(), 8),
      truncateText(determineComplement(transaction), 18)
    };

    float xPosition = margin + 5;
    for (int i = 0; i < rowData.length; i++) {
      // Definir cor baseada no valor (vermelho para negativos)
      if (i == 6 && transaction.getAmount().doubleValue() < 0) {
        contentStream.setNonStrokingColor(1, 0, 0); // Vermelho
      } else {
        contentStream.setNonStrokingColor(0, 0, 0); // Preto
      }

      contentStream.beginText();
      contentStream.newLineAtOffset(xPosition, yPosition);
      contentStream.showText(rowData[i]);
      contentStream.endText();
      xPosition += columnWidths[i];
    }
  }

  private String truncateText(String text, int maxLength) {
    if (text == null) return "";
    return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
  }

  private String determineComplement(Transaction transaction) {
    if (transaction.getHistory() != null && transaction.getHistory().contains("Marlucia")) {
      return "Pagamento de fornecedor";
    }
    if (transaction.getHistory() != null && transaction.getHistory().contains("Alexandre")) {
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
}
