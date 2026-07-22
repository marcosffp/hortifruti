package com.hortifruti.sl.hortifruti.service.billet;

import com.hortifruti.sl.hortifruti.dto.billet.BilletResponse;
import com.hortifruti.sl.hortifruti.model.enumeration.Status;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import com.hortifruti.sl.hortifruti.service.storage.BilletFileStorageService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Confirma no Sicoob, para cada cliente, quais {@link CombinedScore} marcados localmente como
 * pendentes/com boleto realmente ainda estão "Em aberto" — extraído de {@code BilletService} para
 * isolar a regra de conciliação Sicoob da orquestração de listagem/paginação.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SicoobOpenBilletReconciler {

  private final BilletQuery billetQuery;
  private final CombinedScoreService combinedScoreService;
  private final BilletFileStorageService billetFileStorageService;

  public record ReconciledScore(CombinedScore score, boolean confirmadoNoSicoob) {}

  public List<ReconciledScore> reconcileOpenScores(List<CombinedScore> openScores) {
    List<ReconciledScore> reconciledScores = new ArrayList<>();
    Map<Long, List<CombinedScore>> scoresByClient =
        openScores.stream().collect(Collectors.groupingBy(CombinedScore::getClientId));
    for (Map.Entry<Long, List<CombinedScore>> entry : scoresByClient.entrySet()) {
      reconciledScores.addAll(reconcileClientOpenScores(entry.getKey(), entry.getValue()));
    }
    return reconciledScores;
  }

  /**
   * Não confia no status local do CombinedScore: um agrupamento só entra no resultado se o seu
   * boleto (por seuNumero) estiver na lista de "Em aberto" retornada pelo Sicoob.
   *
   * <p>Se a consulta ao Sicoob falhar para o cliente inteiro (ex: falha de comunicação), mantém
   * todos os agrupamentos do cliente na lista, sinalizados como não confirmados — evita esconder
   * boletos por uma falha temporária. Já um agrupamento individual que simplesmente não aparece na
   * lista de "Em aberto" é tratado como encerrado: é removido do resultado, e uma tentativa
   * adicional (best-effort) de consultá-lo diretamente por nossoNumero é feita para corrigir o
   * status local (liquidado/baixado).
   */
  private List<ReconciledScore> reconcileClientOpenScores(
      Long clientId, List<CombinedScore> clientScores) {
    List<BilletResponse> sicoobOpenBillets;
    try {
      sicoobOpenBillets = billetQuery.listBilletByPayer(clientId);
    } catch (Exception e) {
      log.warn(
          "Não foi possível confirmar no Sicoob os boletos em aberto do cliente {}: {}",
          clientId,
          e.getMessage(),
          e);
      return clientScores.stream().map(cs -> new ReconciledScore(cs, false)).toList();
    }

    Set<String> openYourNumbers =
        sicoobOpenBillets.stream()
            .map(BilletResponse::seuNumero)
            .filter(Objects::nonNull)
            .map(String::trim)
            .collect(Collectors.toSet());

    List<ReconciledScore> result = new ArrayList<>();
    for (CombinedScore cs : clientScores) {
      String yourNumber = cs.getYourNumber() == null ? null : cs.getYourNumber().trim();
      if (yourNumber != null && openYourNumbers.contains(yourNumber)) {
        result.add(new ReconciledScore(cs, true));
        continue;
      }

      log.info(
          "Boleto do agrupamento {} (cliente {}) não está na lista de 'Em aberto' do Sicoob —"
              + " removendo da lista de boletos em aberto",
          cs.getId(),
          clientId);
      tryUpdateClosedStatus(cs);
    }

    return result;
  }

  /**
   * Tenta confirmar diretamente no Sicoob (por nossoNumero) a situação final de um agrupamento que
   * não apareceu na lista de "Em aberto" do pagador, para corrigir o status local. Falhas aqui são
   * apenas registradas — o agrupamento já foi excluído da lista de "em aberto" porque a consulta
   * por pagador confirmou que ele não está mais aberto.
   */
  private void tryUpdateClosedStatus(CombinedScore cs) {
    String ourNumber = cs.getOurNumber_sicoob();
    if (ourNumber == null || ourNumber.isBlank()) {
      return;
    }

    try {
      BilletResponse sicoobBillet = billetQuery.getBilletByOurNumber(ourNumber);
      Status resolvedStatus = resolveClosedStatus(sicoobBillet.situacaoBoleto());
      if (resolvedStatus != null) {
        log.info(
            "Boleto do agrupamento {} confirmado como '{}' no Sicoob — atualizando status local"
                + " para {}",
            cs.getId(),
            sicoobBillet.situacaoBoleto(),
            resolvedStatus);
        combinedScoreService.updateStatus(cs.getId(), resolvedStatus);
        if (resolvedStatus == Status.CANCELADO_BOLETO) {
          billetFileStorageService.cancelBilletFileAfterCommit(cs.getId());
        }
      }
    } catch (Exception e) {
      log.warn(
          "Boleto do agrupamento {} não está mais 'Em aberto' no Sicoob, mas não foi possível"
              + " confirmar sua situação final para corrigir o status local: {}",
          cs.getId(),
          e.getMessage(),
          e);
    }
  }

  /**
   * Traduz a situação retornada pelo Sicoob para o status local correspondente, quando o boleto não
   * está mais em aberto. Retorna {@code null} se o boleto ainda estiver em aberto.
   */
  private Status resolveClosedStatus(String situacaoBoleto) {
    String value = situacaoBoleto == null ? "" : situacaoBoleto.toLowerCase();
    if (value.contains("liquidado") || value.contains("pago")) {
      return Status.PAGO;
    }
    if (value.contains("baixado") || value.contains("cancelado")) {
      return Status.CANCELADO_BOLETO;
    }
    return null;
  }
}
