package com.hortifruti.sl.hortifruti.service.invoice;

import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponse;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceResponseGet;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceWithBilletResponse;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceException;
import com.hortifruti.sl.hortifruti.service.billet.BilletService;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Orquestra a emissao de NF seguida da emissao do boleto vinculado a ela, reaproveitando {@link
 * InvoiceService} e {@link BilletService}. O numero do boleto (seuNumero) e derivado do numero
 * fiscal da NF assim que ela e autorizada. Se qualquer etapa apos a emissao da NF falhar (timeout
 * aguardando autorizacao, falha ao baixar DANFE/XML, ou falha ao emitir o boleto), a NF ja emitida
 * e cancelada automaticamente (rollback) para nao deixar uma NF orfa sem boleto vinculado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IssueInvoiceWithBilletService {

  private final InvoiceService invoiceService;
  private final BilletService billetService;

  private static final int MAX_NUMBER_POLL_ATTEMPTS = 12;
  private static final long NUMBER_POLL_INTERVAL_MS = 10_000;

  /**
   * Wrapper {@code @Async} de {@link #issueInvoiceAndBillet} — {@link #waitForInvoiceNumber} pode
   * bloquear por até ~2min em {@code Thread.sleep}, e sem isso essa espera prendia uma thread do
   * servlet (pool limitado do Tomcat) por todo esse tempo. Rodar num executor dedicado libera a
   * thread do servlet para atender outras requisições enquanto esta aguarda; o controller usa
   * {@code CompletableFuture} como tipo de retorno para o Spring MVC manter a requisição HTTP
   * original aberta sem bloquear nada (ver {@code spring.mvc.async.request-timeout}).
   */
  @Async
  public CompletableFuture<InvoiceWithBilletResponse> issueInvoiceAndBilletAsync(
      Long combinedScoreId, String dadosAdicionais, String dueDate) {
    try {
      return CompletableFuture.completedFuture(
          issueInvoiceAndBillet(combinedScoreId, dadosAdicionais, dueDate));
    } catch (RuntimeException e) {
      return CompletableFuture.failedFuture(e);
    }
  }

  public InvoiceWithBilletResponse issueInvoiceAndBillet(
      Long combinedScoreId, String dadosAdicionais, String dueDate) {
    InvoiceResponse invoiceResponse = invoiceService.issueInvoice(combinedScoreId, dadosAdicionais);
    String ref = invoiceResponse.ref();

    try {
      InvoiceResponseGet invoiceInfo = waitForInvoiceNumber(ref);
      String billetNumber = invoiceInfo.number();

      byte[] danfeBytes = extractBytes(invoiceService.downloadDanfe(ref));
      byte[] xmlBytes = extractBytes(invoiceService.downloadXml(ref));

      ResponseEntity<byte[]> billetResponse =
          billetService.generateBillet(combinedScoreId, billetNumber, dueDate);
      byte[] billetBytes = billetResponse.getBody();
      if (billetBytes == null || billetBytes.length == 0) {
        throw new InvoiceException("Boleto gerado sem conteúdo.");
      }

      return new InvoiceWithBilletResponse(
          ref,
          invoiceInfo.number(),
          billetNumber,
          Base64.getEncoder().encodeToString(danfeBytes),
          Base64.getEncoder().encodeToString(xmlBytes),
          Base64.getEncoder().encodeToString(billetBytes));
    } catch (Exception e) {
      log.error(
          "Erro no fluxo combinado NF + Boleto para combinedScoreId={}, invoiceRef={} —"
              + " cancelando a NF automaticamente (rollback)",
          combinedScoreId,
          ref,
          e);
      rollbackInvoice(ref);
      throw new InvoiceException(
          "Erro ao gerar NF e boleto vinculado. A nota fiscal emitida foi cancelada"
              + " automaticamente. Detalhes: "
              + e.getMessage(),
          e);
    }
  }

  private void rollbackInvoice(String ref) {
    try {
      invoiceService.cancelInvoice(
          ref, "Rollback automático: falha ao gerar o boleto vinculado à nota fiscal");
    } catch (Exception cancelException) {
      log.error(
          "Falha ao executar o rollback (cancelamento) da NF ref={} — requer verificação manual",
          ref,
          cancelException);
    }
  }

  private InvoiceResponseGet waitForInvoiceNumber(String ref) {
    InvoiceException lastError = null;

    for (int attempt = 1; attempt <= MAX_NUMBER_POLL_ATTEMPTS; attempt++) {
      sleep(NUMBER_POLL_INTERVAL_MS);
      try {
        InvoiceResponseGet info = invoiceService.consultInvoice(ref);
        if (info.number() != null && !info.number().isBlank()) {
          return info;
        }
      } catch (InvoiceException e) {
        lastError = e;
      }
    }

    throw new InvoiceException(
        "O número da nota fiscal não ficou disponível a tempo para gerar o boleto vinculado.",
        lastError);
  }

  private void sleep(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new InvoiceException("Interrompido ao aguardar autorização da nota fiscal", ie);
    }
  }

  private byte[] extractBytes(ResponseEntity<Resource> response) {
    Resource body = response.getBody();
    if (body == null) {
      throw new InvoiceException("Arquivo da nota fiscal veio vazio.");
    }
    try {
      return body.getInputStream().readAllBytes();
    } catch (IOException e) {
      throw new InvoiceException("Erro ao ler arquivo da nota fiscal: " + e.getMessage(), e);
    }
  }
}
