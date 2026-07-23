package com.hortifruti.sl.hortifruti.service.billet;

import com.hortifruti.sl.hortifruti.dto.billet.BilletRequestSimplified;
import com.hortifruti.sl.hortifruti.dto.billet.BilletResponse;
import com.hortifruti.sl.hortifruti.dto.billet.OpenBilletResponse;
import com.hortifruti.sl.hortifruti.dto.billet.Pagador;
import com.hortifruti.sl.hortifruti.exception.billet.BilletException;
import com.hortifruti.sl.hortifruti.exception.purchase.CombinedScoreException;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.model.purchase.Status;
import com.hortifruti.sl.hortifruti.service.billet.SicoobOpenBilletReconciler.ReconciledScore;
import com.hortifruti.sl.hortifruti.service.purchase.ClientService;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import com.hortifruti.sl.hortifruti.service.storage.BilletFileStorageService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  private final CombinedScoreService combinedScoreService;
  private final ClientService clientService;
  private final BilletFactory billetFactory;
  private final BilletIssue billetIssue;
  private final BilletQuery billetQuery;
  private final BilletCancel billetCancel;
  private final BilletInfoCombinedAndClient billetInfoCombinedAndClient;
  private final BilletFileStorageService billetFileStorageService;
  private final SicoobOpenBilletReconciler sicoobOpenBilletReconciler;

  public List<CombinedScore> findAllPendingWithBilletByClient(Long clientId) {
    return combinedScoreService.findAllPendingWithBilletByClient(clientId);
  }

  public List<CombinedScore> findAllPendingByClient(Long clientId) {
    return combinedScoreService.findAllPendingByClient(clientId);
  }

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
   * não confia nesse status local — a conciliação de fato com o Sicoob é feita por {@link
   * SicoobOpenBilletReconciler}.
   *
   * @return Lista de boletos realmente em aberto, ordenada por vencimento (mais próximos primeiro)
   */
  @Transactional
  public List<OpenBilletResponse> listAllOpenBillets() {
    List<CombinedScore> openScores = combinedScoreService.findAllOpenBillets();
    if (openScores.isEmpty()) {
      return List.of();
    }

    List<ReconciledScore> reconciledScores =
        sicoobOpenBilletReconciler.reconcileOpenScores(openScores);

    List<Long> clientIds =
        reconciledScores.stream().map(rs -> rs.score().getClientId()).distinct().toList();
    Map<Long, String> clientNamesById = clientService.getClientNamesByIds(clientIds);

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

  public ResponseEntity<byte[]> issueCopy(Long idCombinedScore) throws IOException {
    return billetIssue.issueCopy(idCombinedScore);
  }

  public ResponseEntity<String> cancelBillet(Long idCombinedScore)
      throws IOException, BilletException {
    return billetCancel.cancelBillet(idCombinedScore);
  }

  /**
   * Cancelamento manual/avulso de boleto, direto pelo "nosso número" informado pelo operador —
   * sem exigir um agrupamento (CombinedScore) local conhecido.
   */
  public ResponseEntity<String> cancelBilletByNumber(String nossoNumero) throws BilletException {
    return billetCancel.cancelBilletByNumber(nossoNumero);
  }

  public BilletResponse getBilletByCombinedScore(long combinedScoreId) throws IOException {
    return billetQuery.getBilletByCombinedScore(combinedScoreId);
  }

  /**
   * Baixa o PDF do boleto exatamente como foi gerado e guardado no R2, sem emitir uma via nova no
   * Sicoob (diferente de {@link #issueCopy}).
   */
  public ResponseEntity<byte[]> getStoredBilletFile(Long combinedScoreId) {
    CombinedScore combinedScore =
        billetInfoCombinedAndClient.findCombinedScoreById(combinedScoreId);
    byte[] pdfBytes = billetFileStorageService.getBilletFileContent(combinedScoreId);
    return buildPdfResponse(pdfBytes, combinedScore.getYourNumber());
  }

  /**
   * Marca manualmente como pago um agrupamento com boleto (ex: pagamento recebido por fora do
   * Sicoob). Diferente de {@link CombinedScoreService#confirmPayment}, que é para agrupamentos sem
   * boleto, este método é justamente para os que têm boleto — por isso não bloqueia por hasBillet,
   * apenas exige que exista um boleto associado.
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

    combinedScoreService.updateStatus(combinedScoreId, Status.PAGO);
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
    // Trava a linha do agrupamento (SELECT ... FOR UPDATE) para serializar requisições
    // concorrentes: se duas chegarem juntas (ex: duplo clique), a segunda só prossegue depois que
    // a primeira já commitou hasBillet=true, e cai no bloqueio de duplicidade abaixo.
    CombinedScore combinedScore = combinedScoreService.findByIdForUpdate(combinedScoreId);

    if (combinedScore.isHasBillet()) {
      throw new CombinedScoreException(
          "Boleto já foi gerado para este agrupamento (id " + combinedScoreId + ").");
    }

    try {
      Client client = billetInfoCombinedAndClient.findClientById(combinedScore.getClientId());
      Pagador pagador = billetFactory.createPagadorFromClient(client);

      if (dueDate != null && !dueDate.trim().isEmpty()) {
        LocalDate customDueDate = LocalDate.parse(dueDate);
        if (!customDueDate.equals(combinedScore.getDueDate())) {
          combinedScore.setDueDate(customDueDate);
          combinedScoreService.save(combinedScore);
        }
      }

      BilletRequestSimplified billetRequest =
          billetFactory.createBilletRequest(combinedScore, combinedScoreId, pagador, number);
      Map<String, Object> responseBody = issueBilletAndExtractResponse(billetRequest);
      byte[] pdfBytes = (byte[]) responseBody.get("pdf");
      billetFileStorageService.saveBilletFile(combinedScoreId, pdfBytes);
      updateCombinedScoreWithBilletData(combinedScore, responseBody);
      return buildPdfResponse(pdfBytes, combinedScore.getYourNumber());
    } catch (Exception e) {
      throw new CombinedScoreException("Erro ao gerar o boleto: " + e.getMessage(), e);
    }
  }

  @Transactional
  public List<CombinedScore> syncAndFindOverdueUnpaidScores(LocalDate currentDate) {
    List<CombinedScore> overdueScores = combinedScoreService.findOverdueUnpaidScores(currentDate);

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
            combinedScoreService.updateStatus(combinedScore.getId(), Status.PAGO);
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
    combinedScoreService.save(combinedScore);
  }
}
