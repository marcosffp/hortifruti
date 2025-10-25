package com.hortifruti.sl.hortifruti.service.billet;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hortifruti.sl.hortifruti.dto.billet.BilletRequestSimplified;
import com.hortifruti.sl.hortifruti.dto.billet.BilletResponse;
import com.hortifruti.sl.hortifruti.dto.billet.Pagador;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.exception.CombinedScoreException;
import com.hortifruti.sl.hortifruti.model.enumeration.Status;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BilletService {

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
   * Gera um boleto para um CombinedScore específico e retorna o PDF para
   * download.
   *
   * @param combinedScoreId ID do CombinedScore
   * @param number          Número identificador do boleto
   * @return Resposta HTTP contendo o PDF do boleto gerado
   * @throws IOException Se houver erro na comunicação ou no processamento da
   *                     resposta
   */
  @Transactional
  public ResponseEntity<byte[]> generateBillet(Long combinedScoreId, String number)
      throws IOException {
    CombinedScore combinedScore = billetInfoCombinedAndClient.findCombinedScoreById(combinedScoreId);

    try {
      Client client = billetInfoCombinedAndClient.findClientById(combinedScore.getClientId());
      Pagador pagador = billetFactory.createPagadorFromClient(client);
      BilletRequestSimplified billetRequest = billetFactory.createBilletRequest(combinedScore, combinedScoreId, pagador,
          number);
      Map<String, Object> responseBody = issueBilletAndExtractResponse(billetRequest);
      updateCombinedScoreWithBilletData(combinedScore, responseBody);
      return buildPdfResponse((byte[]) responseBody.get("pdf"), combinedScore.getYourNumber());
    } catch (Exception e) {
      throw new CombinedScoreException("Erro ao gerar o boleto: " + e.getMessage(), e);
    }
  }



  @Transactional
  public List<CombinedScore> syncAndFindOverdueUnpaidScores(LocalDate currentDate) {
    // Busca todos os CombinedScore vencidos e não confirmados
    List<CombinedScore> overdueScores = combinedScoreRepository.findOverdueUnpaidScores(currentDate);

    // Lista para armazenar os CombinedScore que permanecem pendentes
    List<CombinedScore> remainingPendingScores = new ArrayList<>(overdueScores);

    for (CombinedScore combinedScore : overdueScores) {
      // Verifica se o CombinedScore possui um boleto associado
      if (combinedScore.isHasBillet() && combinedScore.getStatus() == Status.PENDENTE) {
        try {
          // Busca a lista de boletos atualizada do BilletService
          List<BilletResponse> updatedBillets = listBilletByPayer(combinedScore.getClientId());

          // Verifica se o boleto do CombinedScore está presente na lista retornada
          boolean billetExists = updatedBillets.stream()
              .anyMatch(billet -> billet.seuNumero().equals(combinedScore.getYourNumber()));

          // Se o boleto não estiver presente, considera como pago
          if (!billetExists) {
            combinedScore.setStatus(Status.PAGO);
            combinedScoreRepository.save(combinedScore);

            // Remove o CombinedScore da lista de pendentes
            remainingPendingScores.remove(combinedScore);
          }
        } catch (Exception e) {
          throw new CombinedScoreException(
              "Erro ao sincronizar o status do boleto para o CombinedScore ID "
                  + combinedScore.getId()
                  + ": "
                  + e.getMessage(),
              e);
        }
      }
    }

    // Retorna apenas os CombinedScore que permanecem pendentes e vencidos
    return remainingPendingScores;
  }


   private Map<String, Object> issueBilletAndExtractResponse(BilletRequestSimplified billetRequest) throws IOException {
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

  private void updateCombinedScoreWithBilletData(CombinedScore combinedScore, Map<String, Object> responseBody) {
    String nossoNumero = (String) responseBody.get("nossoNumero");
    String seuNumero = (String) responseBody.get("seuNumero");

    combinedScore.setHasBillet(true);
    combinedScore.setOurNumber_sicoob(nossoNumero);
    combinedScore.setYourNumber(seuNumero);
    combinedScoreRepository.save(combinedScore);
  }
}
