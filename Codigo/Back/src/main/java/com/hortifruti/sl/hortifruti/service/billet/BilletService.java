package com.hortifruti.sl.hortifruti.service.billet;

import com.hortifruti.sl.hortifruti.config.sicoob.SicoobEnvironmentGuard;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
  private final SicoobEnvironmentGuard sicoobEnvironmentGuard;

  /**
   * Ação Sicoob a ser executada se o ambiente não estiver bloqueado. Ver {@link #withSicoobGuard}.
   */
  @FunctionalInterface
  private interface SicoobAction<T, E extends Exception> {
    T execute() throws E;
  }

  /**
   * Extrai o guard {@code if (sicoobEnvironmentGuard.isBlocked()) {...}} que se repetia quase
   * idêntico em cada método público que fala com o Sicoob: se o ambiente está bloqueado (sem
   * integração real, ex: dev/test), loga e retorna {@code blockedValue} sem executar {@code
   * action}.
   */
  private <T, E extends Exception> T withSicoobGuard(
      String methodName, T blockedValue, SicoobAction<T, E> action) throws E {
    if (sicoobEnvironmentGuard.isBlocked()) {
      log.info("[Sicoob] {} bloqueado (ambiente sem integração real).", methodName);
      return blockedValue;
    }
    return action.execute();
  }

  public List<CombinedScore> findAllPendingWithBilletByClient(Long clientId) {
    return combinedScoreService.findAllPendingWithBilletByClient(clientId);
  }

  public List<CombinedScore> findAllPendingByClient(Long clientId) {
    return combinedScoreService.findAllPendingByClient(clientId);
  }

  public List<BilletResponse> listBilletByPayer(long clientId) throws IOException {
    return withSicoobGuard(
        "listBilletByPayer", List.of(), () -> billetQuery.listBilletByPayer(clientId));
  }

  public List<BilletResponse> listBilletByPayer(
      long clientId, Integer codigoSituacao, LocalDate dataInicio, LocalDate dataFim)
      throws IOException {
    return withSicoobGuard(
        "listBilletByPayer",
        List.of(),
        () -> billetQuery.listBilletByPayer(clientId, codigoSituacao, dataInicio, dataFim));
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
    return withSicoobGuard(
        "listAllOpenBillets",
        List.of(),
        () -> {
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
        });
  }

  public ResponseEntity<byte[]> issueCopy(Long idCombinedScore) throws IOException {
    return withSicoobGuard(
        "issueCopy", emptyPdfResponse(), () -> billetIssue.issueCopy(idCombinedScore));
  }

  public ResponseEntity<String> cancelBillet(Long idCombinedScore)
      throws IOException, BilletException {
    return withSicoobGuard(
        "cancelBillet", ResponseEntity.ok(""), () -> billetCancel.cancelBillet(idCombinedScore));
  }

  /**
   * Cancelamento manual/avulso de boleto, direto pelo "nosso número" informado pelo operador — sem
   * exigir um agrupamento (CombinedScore) local conhecido.
   */
  public ResponseEntity<String> cancelBilletByNumber(String nossoNumero) throws BilletException {
    return withSicoobGuard(
        "cancelBilletByNumber",
        ResponseEntity.ok(""),
        () -> billetCancel.cancelBilletByNumber(nossoNumero));
  }

  public BilletResponse getBilletByCombinedScore(long combinedScoreId) throws IOException {
    return withSicoobGuard(
        "getBilletByCombinedScore",
        new BilletResponse("", "", "", "", "", "", BigDecimal.ZERO, combinedScoreId),
        () -> billetQuery.getBilletByCombinedScore(combinedScoreId));
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
    return withSicoobGuard(
        "generateBillet",
        emptyPdfResponse(),
        () -> {
          // Trava a linha do agrupamento (SELECT ... FOR UPDATE) para serializar requisições
          // concorrentes: se duas chegarem juntas (ex: duplo clique), a segunda só prossegue
          // depois que a primeira já commitou hasBillet=true, e cai no bloqueio de duplicidade
          // abaixo.
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
        });
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

  private ResponseEntity<byte[]> emptyPdfResponse() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    return ResponseEntity.ok().headers(headers).body(new byte[0]);
  }

  private void updateCombinedScoreWithBilletData(
      CombinedScore combinedScore, Map<String, Object> responseBody) {
    String nossoNumero = (String) responseBody.get("nossoNumero");
    String seuNumero = (String) responseBody.get("seuNumero");

    combinedScore.setHasBillet(true);
    combinedScore.setOurNumberSicoob(nossoNumero);
    combinedScore.setYourNumber(seuNumero);
    combinedScoreService.save(combinedScore);
  }
}
