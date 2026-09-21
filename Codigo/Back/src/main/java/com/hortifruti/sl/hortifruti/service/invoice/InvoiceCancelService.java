package com.hortifruti.sl.hortifruti.service.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.config.FocusNfeApiClient;
import com.hortifruti.sl.hortifruti.dto.invoice.InvoiceCancelResponse;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceAlreadyCancelledException;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceException;
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

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final FocusNfeApiClient focusNfeApiClient;

  private final InvoiceCancellationRecordUpdater recordUpdater;

  private final FiscalNoteCancellationPoller cancellationPoller;

  public InvoiceCancelResponse cancelInvoice(String ref, String justificativa) {
    return cancelInvoice(ref, justificativa, false);
  }

  /**
   * Cancela a NF-e na Focus NFe. Quando {@code extemporaneo} é verdadeiro, a justificativa é
   * marcada como cancelamento extemporâneo (fora do prazo normal de ~24h da SEFAZ, permitido em
   * caráter excepcional mediante processo administrativo próprio — a chamada à Focus NFe é a mesma,
   * a diferença é apenas de prazo/autorização já obtida pelo operador).
   *
   * <p>A Focus NFe pode confirmar o cancelamento já na resposta síncrona do DELETE (status {@code
   * cancelado}) ou deixá-lo em processamento assíncrono (status {@code processando_cancelamento} —
   * a confirmação real, ou a rejeição pela SEFAZ, só chega depois). Só no primeiro caso os
   * registros locais são marcados como cancelados aqui; no segundo, {@link
   * FiscalNoteCancellationPoller} confirma antes de tocar no registro local — sem essa checagem, o
   * sistema reportaria "cancelado com sucesso" para uma nota que a SEFAZ pode acabar rejeitando.
   */
  @Transactional
  public InvoiceCancelResponse cancelInvoice(String ref, String justificativa, boolean extemporaneo) {
    try {
      String finalJustificativa =
          extemporaneo ? "[CANCELAMENTO EXTEMPORANEO] " + justificativa : justificativa;
      String response = focusNfeApiClient.cancelInvoice(ref, finalJustificativa);
      return handleCancelResponse(ref, response);
    } catch (InvoiceAlreadyCancelledException e) {
      log.info(e.getMessage());
      recordUpdater.updateLocalRecordsBestEffort(ref);
      return new InvoiceCancelResponse(ref, "CANCELADO", e.getMessage());
    } catch (Exception e) {
      throw new InvoiceException("Erro ao cancelar a NF-e: " + e.getMessage(), e);
    }
  }

  private InvoiceCancelResponse handleCancelResponse(String ref, String rawResponse) {
    String status = extractStatus(rawResponse);

    if (isConfirmedCancelled(status)) {
      recordUpdater.updateLocalRecordsBestEffort(ref);
      return new InvoiceCancelResponse(ref, "CANCELADO", "Nota fiscal cancelada com sucesso.");
    }

    cancellationPoller.triggerConfirmationAfterCancel(ref);
    return new InvoiceCancelResponse(
        ref,
        "PROCESSANDO",
        "Cancelamento enviado à SEFAZ e está em processamento (status: "
            + status
            + "). A confirmação pode levar alguns minutos.");
  }

  /**
   * Sem {@code status} no corpo (formato inesperado da Focus NFe) mantém o comportamento anterior
   * — assume cancelamento confirmado — em vez de travar o fluxo por um detalhe de parsing.
   */
  private boolean isConfirmedCancelled(String status) {
    if (status == null || status.isBlank()) {
      return true;
    }
    return status.toLowerCase().contains("cancelado");
  }

  private String extractStatus(String rawResponse) {
    try {
      JsonNode node = objectMapper.readTree(rawResponse);
      return node.path("status").asText(null);
    } catch (Exception e) {
      return null;
    }
  }
}
