package com.hortifruti.sl.hortifruti.service.billet;

import com.hortifruti.sl.hortifruti.dto.billet.BilletRequestSimplified;
import com.hortifruti.sl.hortifruti.dto.billet.BilletResponse;
import com.hortifruti.sl.hortifruti.dto.billet.Pagador;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.exception.CombinedScoreException;
import com.hortifruti.sl.hortifruti.model.enumeration.Status;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final BilletFactory billetFactory;
  private final BilletIssue billetIssue;
  private final BilletQuery billetQuery;
  private final BilletCancel billetCancel;
  private final BilletInfoCombinedAndClient billetInfoCombinedAndClient;

  public List<BilletResponse> listBilletByPayer(long clientId) throws IOException {
    return billetQuery.listBilletByPayer(clientId);
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
