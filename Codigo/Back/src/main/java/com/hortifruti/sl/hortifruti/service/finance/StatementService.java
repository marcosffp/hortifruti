package com.hortifruti.sl.hortifruti.service.finance;

import com.hortifruti.sl.hortifruti.dto.bb.BBImportSummary;
import com.hortifruti.sl.hortifruti.dto.sicoob.SicoobImportSummary;
import com.hortifruti.sl.hortifruti.dto.transaction.StatementResponse;
import com.hortifruti.sl.hortifruti.model.finance.Statement;
import com.hortifruti.sl.hortifruti.repository.finance.StatementRepository;
import com.hortifruti.sl.hortifruti.service.storage.R2StorageService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Ponto de entrada único para consulta/geração de extratos bancários. A orquestração específica de
 * cada banco (chamada HTTP, geração de PDF/Excel, import) fica em {@link SicoobStatementService} e
 * {@link BBStatementService}; esta classe só cuida do que é comum a qualquer extrato (listagem,
 * download do arquivo já salvo).
 */
@Service
@RequiredArgsConstructor
public class StatementService {

  private final StatementRepository statementRepository;
  private final R2StorageService r2StorageService;
  private final SicoobStatementService sicoobStatementService;
  private final BBStatementService bbStatementService;

  public List<StatementResponse> listAll() {
    return statementRepository.findAll().stream()
        .map(
            s ->
                new StatementResponse(
                    s.getId(), s.getName(), s.getBank(), s.getOrigin(), s.getCreatedAt()))
        .collect(Collectors.toList());
  }

  /**
   * Lê o conteúdo do arquivo do extrato, do R2 (registros novos) ou da coluna legada (registros
   * salvos antes da migração para R2).
   */
  public byte[] getFileContent(Statement statement) {
    if (statement.getObjectKey() != null) {
      return r2StorageService.download(statement.getObjectKey());
    }
    return statement.getFilePath();
  }

  public SicoobImportSummary importFromSicoobApi(
      int mes, int ano, Integer diaInicial, Integer diaFinal) throws IOException {
    return sicoobStatementService.importFromSicoobApi(mes, ano, diaInicial, diaFinal);
  }

  public byte[] exportSicoobExtratoPdf(int mes, int ano, Integer diaInicial, Integer diaFinal)
      throws IOException {
    return sicoobStatementService.exportSicoobExtratoPdf(mes, ano, diaInicial, diaFinal);
  }

  public byte[] exportSicoobExtratoExcel(int mes, int ano, Integer diaInicial, Integer diaFinal)
      throws IOException {
    return sicoobStatementService.exportSicoobExtratoExcel(mes, ano, diaInicial, diaFinal);
  }

  public BBImportSummary importFromBBApi(LocalDate dataInicio, LocalDate dataFim)
      throws IOException {
    return bbStatementService.importFromBBApi(dataInicio, dataFim);
  }

  public byte[] exportBBExtratoPdf(LocalDate dataInicio, LocalDate dataFim) throws IOException {
    return bbStatementService.exportBBExtratoPdf(dataInicio, dataFim);
  }

  public byte[] exportBBExtratoExcel(LocalDate dataInicio, LocalDate dataFim) throws IOException {
    return bbStatementService.exportBBExtratoExcel(dataInicio, dataFim);
  }
}
