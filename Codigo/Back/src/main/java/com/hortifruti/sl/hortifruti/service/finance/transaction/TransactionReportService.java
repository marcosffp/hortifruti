package com.hortifruti.sl.hortifruti.service.finance.transaction;

import com.hortifruti.sl.hortifruti.model.finance.Transaction;
import com.hortifruti.sl.hortifruti.repository.finance.TransactionRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Relatório consolidado de transações: junta BB e Sicoob (não importa a origem, PDF_UPLOAD ou API)
 * num único PDF/Excel, em ordem cronológica, com descrição e categoria lado a lado.
 */
@Service
@RequiredArgsConstructor
public class TransactionReportService {

  private static final DateTimeFormatter FILE_NAME_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd-MM-yyyy");

  private final TransactionRepository transactionRepository;
  private final TransactionReportPdfGenerator pdfGenerator;
  private final TransactionReportExcelGenerator excelGenerator;

  /** Nome de arquivo padrão do relatório bancário consolidado (BB + Sicoob), em PDF ou Excel. */
  public String buildFileName(LocalDate dataInicio, LocalDate dataFim, String extensao) {
    LocalDate[] periodo = resolvePeriodo(dataInicio, dataFim);
    return "Relatorio_Bancario-"
        + periodo[0].format(FILE_NAME_DATE_FORMATTER)
        + "_a_"
        + periodo[1].format(FILE_NAME_DATE_FORMATTER)
        + "."
        + extensao;
  }

  public byte[] generatePdf(LocalDate dataInicio, LocalDate dataFim) throws IOException {
    LocalDate[] periodo = resolvePeriodo(dataInicio, dataFim);
    List<Transaction> transacoes = buscarTransacoesOrdenadas(periodo[0], periodo[1]);
    return pdfGenerator.generate(periodo[0], periodo[1], transacoes);
  }

  public byte[] generateExcel(LocalDate dataInicio, LocalDate dataFim) throws IOException {
    LocalDate[] periodo = resolvePeriodo(dataInicio, dataFim);
    List<Transaction> transacoes = buscarTransacoesOrdenadas(periodo[0], periodo[1]);
    return excelGenerator.generate(transacoes);
  }

  private List<Transaction> buscarTransacoesOrdenadas(LocalDate dataInicio, LocalDate dataFim) {
    return transactionRepository.findByTransactionDateBetweenOrderByTransactionDateAscIdAsc(
        dataInicio, dataFim);
  }

  private LocalDate[] resolvePeriodo(LocalDate dataInicio, LocalDate dataFim) {
    LocalDate now = LocalDate.now();
    LocalDate inicio = dataInicio != null ? dataInicio : now.minusMonths(1).withDayOfMonth(1);
    LocalDate fim = dataFim != null ? dataFim : now.withDayOfMonth(1).minusDays(1);
    return new LocalDate[] {inicio, fim};
  }
}
