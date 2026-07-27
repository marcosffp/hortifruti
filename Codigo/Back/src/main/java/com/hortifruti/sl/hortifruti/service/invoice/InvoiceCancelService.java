package com.hortifruti.sl.hortifruti.service.invoice;

import com.hortifruti.sl.hortifruti.config.FocusNfeApiClient;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceAlreadyCancelledException;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceException;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class InvoiceCancelService {

  /**
   * Justificativa fixa usada em todo cancelamento de NF-e disparado pelo usuário via UI (botão "Dar
   * Baixa"/"Cancelar Nota Fiscal" e cancelamento manual/avulso). O usuário não escolhe mais o texto
   * — evita cancelamentos com justificativa mal preenchida ou curta demais para a Focus NFe.
   */
  public static final String MANUAL_CANCEL_JUSTIFICATIVA = "Cancelamento extemporâneo";

  private final FocusNfeApiClient focusNfeApiClient;

  private final CombinedScoreService combinedScoreService;

  private final FiscalNoteXmlStorageService fiscalNoteXmlStorageService;

  public String cancelInvoice(String ref, String justificativa) {
    return cancelInvoice(ref, justificativa, false);
  }

  /**
   * Cancela a NF-e na Focus NFe. Quando {@code extemporaneo} é verdadeiro, a justificativa é
   * marcada como cancelamento extemporâneo (fora do prazo normal de ~24h da SEFAZ, permitido em
   * caráter excepcional mediante processo administrativo próprio — a chamada à Focus NFe é a mesma,
   * a diferença é apenas de prazo/autorização já obtida pelo operador).
   *
   * <p>A atualização do registro local (CombinedScore/XML armazenado) é best-effort: usado também
   * pelo cancelamento manual/avulso, quando pode não existir nenhum registro local vinculado à ref
   * informada — nesse caso a NF já foi cancelada de fato na Focus NFe e isso não deve ser reportado
   * como falha.
   */
  @Transactional
  public String cancelInvoice(String ref, String justificativa, boolean extemporaneo) {
    try {
      String finalJustificativa =
          extemporaneo ? "[CANCELAMENTO EXTEMPORANEO] " + justificativa : justificativa;
      String response = focusNfeApiClient.cancelInvoice(ref, finalJustificativa);
      updateLocalRecordsBestEffort(ref);
      return response;
    } catch (InvoiceAlreadyCancelledException e) {
      log.info(e.getMessage());
      updateLocalRecordsBestEffort(ref);
      return e.getMessage();
    } catch (Exception e) {
      throw new InvoiceException("Erro ao cancelar a NF-e: " + e.getMessage(), e);
    }
  }

  private void updateLocalRecordsBestEffort(String ref) {
    try {
      if (combinedScoreService.findByInvoiceRef(ref).isEmpty()) {
        log.info(
            "NF-e {} cancelada na Focus NFe sem CombinedScore local correspondente (cancelamento"
                + " avulso).",
            ref);
      } else {
        combinedScoreService.updateStatusAfterInvoiceCancellation(ref);
      }
    } catch (Exception e) {
      log.warn(
          "NF-e {} cancelada na Focus NFe, mas falhou ao atualizar o registro local"
              + " correspondente.",
          ref,
          e);
    }
    try {
      fiscalNoteXmlStorageService.cancelXmlFileAfterCommit(ref);
    } catch (Exception e) {
      log.warn("Não foi possível marcar o XML armazenado da NF-e {} como cancelado.", ref, e);
    }
  }
}
