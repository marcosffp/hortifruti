package com.hortifruti.sl.hortifruti.service.billet;

import com.hortifruti.sl.hortifruti.dto.billet.BilletRequestSimplified;
import com.hortifruti.sl.hortifruti.dto.billet.BilletResponse;
import com.hortifruti.sl.hortifruti.dto.billet.OpenBilletResponse;
import com.hortifruti.sl.hortifruti.dto.billet.Pagador;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.exception.CombinedScoreException;
import com.hortifruti.sl.hortifruti.model.enumeration.Status;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BilletService {

  public List<CombinedScore> findAllPendingWithBilletByClient(Long clientId) {
    return combinedScoreRepository.findAllPendingWithBilletByClient(clientId);
  }

  public List<CombinedScore> findAllPendingByClient(Long clientId) {
    return combinedScoreRepository.findAllPendingByClient(clientId);
  }

  private final CombinedScoreRepository combinedScoreRepository;
  private final ClientRepository clientRepository;
  private final BilletFactory billetFactory;
  private final BilletIssue billetIssue;
  private final BilletQuery billetQuery;
  private final BilletCancel billetCancel;
  private final BilletInfoCombinedAndClient billetInfoCombinedAndClient;

  public List<BilletResponse> listBilletByPayer(long clientId) throws IOException {
    return billetQuery.listBilletByPayer(clientId);
  }

  public List<BilletResponse> listBilletByPayer(
      long clientId, Integer codigoSituacao, LocalDate dataInicio, LocalDate dataFim)
      throws IOException {
    return billetQuery.listBilletByPayer(clientId, codigoSituacao, dataInicio, dataFim);
  }

  /**
   * Lista todos os boletos em aberto de todos os clientes, ordenados por data de vencimento.
   *
   * <p>Parte dos agrupamentos (CombinedScore) marcados localmente como pendentes/com boleto, mas
   * não confia nesse status local — para cada cliente, busca no Sicoob a lista de boletos
   * realmente "Em aberto" (mesma consulta usada pela tela "Consultar por Cliente") e só inclui na
   * lista final os agrupamentos cujo boleto aparece nela. Agrupamentos que não aparecem são
   * considerados encerrados no Sicoob: o status local é corrigido quando é possível confirmar a
   * situação final (liquidado/baixado) e, de todo modo, são removidos da lista de "em aberto".
   *
   * @return Lista de boletos realmente em aberto, ordenada por vencimento (mais próximos primeiro)
   */
  @Transactional
  public List<OpenBilletResponse> listAllOpenBillets() {
    List<CombinedScore> openScores = combinedScoreRepository.findAllOpenBillets();
    if (openScores.isEmpty()) {
      return List.of();
    }

    List<ReconciledScore> reconciledScores = new ArrayList<>();
    Map<Long, List<CombinedScore>> scoresByClient =
        openScores.stream().collect(Collectors.groupingBy(CombinedScore::getClientId));
    for (Map.Entry<Long, List<CombinedScore>> entry : scoresByClient.entrySet()) {
      reconciledScores.addAll(reconcileClientOpenScores(entry.getKey(), entry.getValue()));
    }

    List<Long> clientIds =
        reconciledScores.stream().map(rs -> rs.score().getClientId()).distinct().toList();
    Map<Long, String> clientNamesById =
        clientRepository.findAllById(clientIds).stream()
            .collect(Collectors.toMap(Client::getId, Client::getClientName));

    return reconciledScores.stream()
        .map(
            rs -> {
              CombinedScore cs = rs.score();
              return new OpenBilletResponse(
                  cs.getId(),
                  cs.getClientId(),
                  clientNamesById.getOrDefault(cs.getClientId(), "Cliente não encontrado"),
                  cs.getTotalValue(),
                  cs.getDueDate(),
                  cs.getYourNumber(),
                  rs.confirmadoNoSicoob());
            })
        .sorted(
            Comparator.comparing(
                OpenBilletResponse::dueDate, Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  private record ReconciledScore(CombinedScore score, boolean confirmadoNoSicoob) {}

  /**
   * Confirma no Sicoob quais agrupamentos de um cliente realmente estão "Em aberto", usando a
   * mesma consulta da tela "Consultar por Cliente" (codigoSituacao=1). Não confia no status local
   * do CombinedScore: um agrupamento só entra no resultado se o seu boleto (por seuNumero) estiver
   * na lista de "Em aberto" retornada pelo Sicoob.
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
          e.getMessage());
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
   * Tenta confirmar diretamente no Sicoob (por nossoNumero) a situação final de um agrupamento
   * que não apareceu na lista de "Em aberto" do pagador, para corrigir o status local. Falhas
   * aqui são apenas registradas — o agrupamento já foi excluído da lista de "em aberto" porque a
   * consulta por pagador confirmou que ele não está mais aberto.
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
        cs.setStatus(resolvedStatus);
        combinedScoreRepository.save(cs);
      }
    } catch (Exception e) {
      log.warn(
          "Boleto do agrupamento {} não está mais 'Em aberto' no Sicoob, mas não foi possível"
              + " confirmar sua situação final para corrigir o status local: {}",
          cs.getId(),
          e.getMessage());
    }
  }

  /**
   * Traduz a situação retornada pelo Sicoob para o status local correspondente, quando o boleto
   * não está mais em aberto. Retorna {@code null} se o boleto ainda estiver em aberto.
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

  public ResponseEntity<byte[]> issueCopy(Long idCombinedScore) throws IOException {
    return billetIssue.issueCopy(idCombinedScore);
  }

  public ResponseEntity<String> cancelBillet(Long idCombinedScore)
      throws IOException, BilletException {
    return billetCancel.cancelBillet(idCombinedScore);
  }

  public BilletResponse getBilletByCombinedScore(long combinedScoreId) throws IOException {
    return billetQuery.getBilletByCombinedScore(combinedScoreId);
  }

  /**
   * Marca manualmente como pago um agrupamento com boleto (ex: pagamento recebido por fora do
   * Sicoob). Diferente de {@link CombinedScoreService#confirmPayment}, que é para agrupamentos
   * sem boleto, este método é justamente para os que têm boleto — por isso não bloqueia por
   * hasBillet, apenas exige que exista um boleto associado.
   */
  @Transactional
  public void markBilletAsPaid(Long combinedScoreId) {
    CombinedScore combinedScore =
        billetInfoCombinedAndClient.findCombinedScoreById(combinedScoreId);

    if (!combinedScore.isHasBillet()) {
      throw new CombinedScoreException("Agrupamento não possui boleto associado.");
    }

    if (combinedScore.getStatus() == Status.PAGO) {
      throw new CombinedScoreException("O pagamento deste agrupamento já foi confirmado.");
    }

    combinedScore.setStatus(Status.PAGO);
    combinedScoreRepository.save(combinedScore);
  }

  /**
   * Gera um boleto para um CombinedScore específico e retorna o PDF para download.
   *
   * @param combinedScoreId ID do CombinedScore
   * @param number Número identificador do boleto
   * @param dueDate Data de vencimento opcional como String (formato yyyy-MM-dd) - se null, usa a
   *     data padrão do CombinedScore
   * @return Resposta HTTP contendo o PDF do boleto gerado
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  @Transactional
  public ResponseEntity<byte[]> generateBillet(Long combinedScoreId, String number, String dueDate)
      throws IOException {
    CombinedScore combinedScore =
        billetInfoCombinedAndClient.findCombinedScoreById(combinedScoreId);

    try {
      Client client = billetInfoCombinedAndClient.findClientById(combinedScore.getClientId());
      Pagador pagador = billetFactory.createPagadorFromClient(client);

      if (dueDate != null && !dueDate.trim().isEmpty()) {
        LocalDate customDueDate = LocalDate.parse(dueDate);
        if (!customDueDate.equals(combinedScore.getDueDate())) {
          combinedScore.setDueDate(customDueDate);
          combinedScoreRepository.save(combinedScore);
        }
      }

      BilletRequestSimplified billetRequest =
          billetFactory.createBilletRequest(combinedScore, combinedScoreId, pagador, number);
      Map<String, Object> responseBody = issueBilletAndExtractResponse(billetRequest);
      updateCombinedScoreWithBilletData(combinedScore, responseBody);
      return buildPdfResponse((byte[]) responseBody.get("pdf"), combinedScore.getYourNumber());
    } catch (Exception e) {
      throw new CombinedScoreException("Erro ao gerar o boleto: " + e.getMessage(), e);
    }
  }

  @Transactional
  public List<CombinedScore> syncAndFindOverdueUnpaidScores(LocalDate currentDate) {
    List<CombinedScore> overdueScores =
        combinedScoreRepository.findOverdueUnpaidScores(currentDate);

    List<CombinedScore> remainingPendingScores = new ArrayList<>();

    for (CombinedScore combinedScore : overdueScores) {
      boolean shouldRemainPending = true;

      if (combinedScore.isHasBillet() && combinedScore.getStatus() == Status.PENDENTE) {
        try {
          List<BilletResponse> updatedBillets = listBilletByPayer(combinedScore.getClientId());

          Optional<BilletResponse> currentBillet =
              updatedBillets.stream()
                  .filter(billet -> billet.seuNumero().equals(combinedScore.getYourNumber()))
                  .findFirst();

          if (currentBillet.isEmpty()) {
            combinedScore.setStatus(Status.PAGO);
            combinedScoreRepository.save(combinedScore);
            shouldRemainPending = false;
          }
        } catch (Exception e) {
          shouldRemainPending = true;
        }
      }

      if (shouldRemainPending) {
        remainingPendingScores.add(combinedScore);
      }
    }

    return remainingPendingScores;
  }

  private Map<String, Object> issueBilletAndExtractResponse(BilletRequestSimplified billetRequest)
      throws IOException {
    ResponseEntity<Map<String, Object>> billetResponse = billetIssue.issueBillet(billetRequest);
    Map<String, Object> responseBody = billetResponse.getBody();

    if (responseBody == null) {
      throw new CombinedScoreException("Erro ao processar a resposta da API: corpo vazio.");
    }

    return responseBody;
  }

  private ResponseEntity<byte[]> buildPdfResponse(byte[] pdfBytes, String yourNumber) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "BOL-" + yourNumber + ".pdf");

    return ResponseEntity.ok().headers(headers).body(pdfBytes);
  }

  private void updateCombinedScoreWithBilletData(
      CombinedScore combinedScore, Map<String, Object> responseBody) {
    String nossoNumero = (String) responseBody.get("nossoNumero");
    String seuNumero = (String) responseBody.get("seuNumero");

    combinedScore.setHasBillet(true);
    combinedScore.setOurNumber_sicoob(nossoNumero);
    combinedScore.setYourNumber(seuNumero);
    combinedScoreRepository.save(combinedScore);
  }
}
