package com.hortifruti.sl.hortifruti.service.billet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.config.billet.BilletHttpClient;
import com.hortifruti.sl.hortifruti.dto.billet.BilletRequest;
import com.hortifruti.sl.hortifruti.dto.billet.BilletRequestSimplified;
import com.hortifruti.sl.hortifruti.dto.billet.BilletResponse;
import com.hortifruti.sl.hortifruti.dto.billet.Pagador;
import com.hortifruti.sl.hortifruti.exception.BilletException;
import com.hortifruti.sl.hortifruti.exception.CombinedScoreException;
import com.hortifruti.sl.hortifruti.model.enumeration.Status;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.model.purchase.CombinedScore;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import com.hortifruti.sl.hortifruti.repository.purchase.CombinedScoreRepository;
import com.hortifruti.sl.hortifruti.service.purchase.CombinedScoreService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class BilletService {

  private final CombinedScoreRepository combinedScoreRepository;
  private final ClientRepository clientRepository;
  private final BilletFactory billetFactory;
  private final PdfCreate pdfCreate;
  private final CombinedScoreService combinedScoreService;

  @Value("${sicoob.num.cliente}")
  private Integer clientNumber;

  @Value("${sicoob.num.conta.corrente}")
  private Integer accountNumber;

  private Integer MODALITY_CODE = 1;
  private String BASE_URL = "/cobranca-bancaria/v3/";
  private final BilletHttpClient httpClient;

  /**
   * Emite um boleto através da API do Sicoob e retorna o PDF para download.
   *
   * @param boleto Dados simplificados do boleto
   * @return Resposta HTTP contendo o PDF do boleto emitido
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  private ResponseEntity<Map<String, Object>> issueBillet(BilletRequestSimplified boleto) throws IOException {
    try {
        // Cria o objeto BoletoRequest completo
        BilletRequest boletoCompleto = billetFactory.createCompleteBoletoRequest(boleto);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode boletoJson = mapper.valueToTree(boletoCompleto);

        // Faz a requisição para emitir o boleto
        JsonNode resposta = httpClient.post(BASE_URL + "boletos", boletoJson);

        // Verifica se a resposta é nula ou vazia
        if (resposta == null || resposta.isEmpty()) {
            throw new BilletException("A resposta da API está vazia ou nula.");
        }

        // Acessa o campo "resultado"
        JsonNode resultado = resposta.path("resultado");

        // Verifica se o campo pdfBoleto está presente e não vazio
        if (!resultado.has("pdfBoleto")
                || resultado.get("pdfBoleto").isNull()
                || resultado.get("pdfBoleto").asText().trim().isEmpty()) {
            throw new BilletException("PDF do boleto não encontrado na resposta.");
        }

        // Extrai o PDF do boleto em Base64
        String pdfBase64 = resultado.get("pdfBoleto").asText();

        // Extrai os valores de nossoNumero e seuNumero
        String nossoNumero = resultado.path("nossoNumero").asText();
        String seuNumero = resultado.path("seuNumero").asText();

        // Converte o PDF Base64 para bytes e prepara a resposta
        byte[] pdfBytes = pdfCreate.convertBase64ToBytes(pdfBase64);

        // Cria um mapa para incluir o PDF e os valores adicionais
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("pdf", pdfBytes);
        responseMap.put("nossoNumero", nossoNumero);
        responseMap.put("seuNumero", seuNumero);

        return ResponseEntity.ok(responseMap);

    } catch (HttpClientErrorException e) {
        throw new BilletException(
                "Erro na requisição para emitir o boleto: " + e.getResponseBodyAsString(), e);
    } catch (IOException e) {
        throw new BilletException("Erro ao processar a resposta da API ao emitir o boleto.", e);
    } catch (Exception e) {
        throw new BilletException("Erro inesperado ao emitir o boleto.", e);
    }
  }

  /**
   * Lista os boletos de um pagador específico.
   *
   * @param clientId ID do cliente (CPF ou CNPJ)
   * @return Lista de boletos do pagador
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public List<BilletResponse> listBilletByPayer(long clientId) throws IOException {
    String numeroCpfCnpj = getClientById(clientId).getDocument();
    try {
      // Monta o endpoint para a requisição
      String endpoint =
          String.format(
              BASE_URL + "pagadores/%s/boletos?numeroCliente=%d&codigoSituacao=1",
              numeroCpfCnpj,
              clientNumber);

      // Faz a requisição para obter os boletos
      ResponseEntity<JsonNode> response = httpClient.getWithResponse(endpoint);

      // Verifica o status da resposta
      if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
        return List.of(); // Retorna lista vazia
      }

      JsonNode resposta = response.getBody();

      // Verifica se a resposta é nula ou vazia
      if (resposta == null || resposta.isEmpty()) {
        return List.of(); // Retorna lista vazia
      }

      // Acessa o campo "resultado" que contém a lista de boletos
      JsonNode resultado = resposta.path("resultado");

      // Verifica se o resultado é uma lista válida
      if (!resultado.isArray()) {
        throw new BilletException("Resposta inválida da API: campo 'resultado' não é uma lista.");
      }

      // Mapeia os dados para uma lista de BoletoResponse
      List<BilletResponse> boletos = new ArrayList<>();
      for (JsonNode boletoNode : resultado) {
        BilletResponse boleto =
            new BilletResponse(
                boletoNode.path("pagador").path("nome").asText(),
                boletoNode.path("dataEmissao").asText(),
                boletoNode.path("dataVencimento").asText(),
                boletoNode.path("seuNumero").asText(),
                boletoNode.path("situacaoBoleto").asText(),
                boletoNode.path("valor").decimalValue());
        boletos.add(boleto);
      }

      return boletos;

    } catch (HttpClientErrorException e) {
      throw new BilletException(
          "Erro na requisição para listar boletos: " + e.getResponseBodyAsString(), e);
    } catch (IOException e) {
      throw new BilletException("Erro ao processar a resposta da API ao listar boletos.", e);
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao listar boletos.", e);
    }
  }

  /**
   * Emite a segunda via de um boleto e retorna o PDF através da API do Sicoob.
   *
   * @param nossoNumero Número identificador do boleto no Sisbr
   * @return Resposta da API contendo o PDF do boleto emitido
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public ResponseEntity<byte[]> issueCopy(Long idCombinedScore) throws IOException {
    // Monta o endpoint para a requisição
    CombinedScore combinedScore =
        combinedScoreRepository
            .findById(idCombinedScore)
            .orElseThrow(
                () ->
                    new CombinedScoreException(
                        "Agrupamento com o ID " + idCombinedScore + " não encontrado."));
    if (combinedScore.isHasBillet() == false) {
      throw new CombinedScoreException("Agrupamento não possui boleto associado.");
    }

    String nossoNumero = combinedScore.getOurNumber_sicoob();
    String endpoint =
        String.format(
            BASE_URL
                + "boletos/segunda-via?numeroCliente=%d&codigoModalidade=%d&nossoNumero=%s&gerarPdf=true",
            clientNumber,
            MODALITY_CODE,
            nossoNumero);

    try {
      // Faz a requisição GET para a API
      JsonNode resposta = httpClient.get(endpoint);

      // Verifica se a resposta é nula ou vazia
      if (resposta == null || resposta.isEmpty()) {
        throw new BilletException("A resposta da API está vazia ou nula.");
      }

      // Acessa o campo "resultado"
      JsonNode resultado = resposta.path("resultado");

      // Verifica se o campo "pdfBoleto" está presente e não vazio
      if (!resultado.has("pdfBoleto")
          || resultado.get("pdfBoleto").isNull()
          || resultado.get("pdfBoleto").asText().trim().isEmpty()) {
        throw new BilletException("PDF do boleto não encontrado na resposta.");
      }

      // Extrai o PDF do boleto em Base64
      String pdfBase64 = resultado.get("pdfBoleto").asText();

      // Converte o PDF Base64 para bytes e prepara a resposta
      return pdfCreate.createResponsePdf(pdfBase64, "SEGUNDA-VIA-BOL-" + nossoNumero + ".pdf");

    } catch (HttpClientErrorException.NotFound e) {
      throw new BilletException(
          "Boleto não encontrado. Verifique o 'nossoNumero' e tente novamente.", e);
    } catch (IOException e) {
      throw new BilletException(
          "Erro ao processar a resposta da API ao emitir a segunda via do boleto.", e);
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao emitir a segunda via do boleto.", e);
    }
  }

  /**
   * Realiza a baixa (cancelamento) de um boleto através da API do Sicoob.
   *
   * @param nossoNumero Número identificador do boleto no Sisbr
   * @return Resposta indicando o sucesso ou falha da operação
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   * @throws BilletException Se houver erro específico da API de boletos
   */
  public ResponseEntity<String> cancelBillet(Long idCombinedScore)
      throws IOException, BilletException {
    CombinedScore combinedScore =
        combinedScoreRepository
            .findById(idCombinedScore)
            .orElseThrow(
                () ->
                    new CombinedScoreException(
                        "Agrupamento com o ID " + idCombinedScore + " não encontrado."));
    if (combinedScore.isHasBillet() == false) {
      throw new CombinedScoreException("Agrupamento não possui boleto associado.");
    }
    try {
      // Monta o objeto de requisição para a baixa do boleto
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("numeroCliente", clientNumber);
      requestBody.put("codigoModalidade", MODALITY_CODE);
      String nossoNumero = combinedScore.getOurNumber_sicoob();

      // Monta o endpoint com o número do boleto
      String endpoint = String.format(BASE_URL + "boletos/%s/baixar", nossoNumero);

      // Faz a requisição POST para realizar a baixa do boleto
      httpClient.post(endpoint, requestBody);

      // Atualiza o status do CombinedScore após o cancelamento do boleto
      combinedScoreService.updateStatusAfterBilletCancellation(nossoNumero);

      // Se chegou até aqui, a operação foi bem-sucedida (código 204)
      return ResponseEntity.noContent().build();

    } catch (HttpClientErrorException.BadRequest e) {
      // Extrai a mensagem detalhada de erro para códigos 400
      String errorBody = e.getResponseBodyAsString();

      // Se o erro contém "Título em processo de baixa/liquidação", trata como sucesso
      if (errorBody.contains("Título em processo de baixa/liquidação")) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body("O boleto já está em processo de cancelamento ou já foi liquidado.");
      }

      throw new BilletException("Erro na requisição para baixar o boleto: " + errorBody);
    } catch (HttpClientErrorException e) {
      // Para outros erros HTTP do cliente
      throw new BilletException(
          "Erro na requisição para baixar o boleto: " + e.getResponseBodyAsString());
    } catch (IOException e) {
      throw new BilletException("Erro de comunicação ao baixar o boleto: " + e.getMessage());
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao baixar o boleto: " + e.getMessage());
    }
  }

  /**
   * Gera um boleto para um CombinedScore específico e retorna o PDF para download.
   *
   * @param combinedScoreId ID do CombinedScore
   * @param number Número identificador do boleto
   * @return Resposta HTTP contendo o PDF do boleto gerado
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
@Transactional
public ResponseEntity<byte[]> generateBillet(Long combinedScoreId, String number) throws IOException {
    // Busca o agrupamento pelo ID
    CombinedScore combinedScore =
        combinedScoreRepository
            .findById(combinedScoreId)
            .orElseThrow(
                () ->
                    new CombinedScoreException(
                        "Agrupamento com o ID " + combinedScoreId + " não encontrado."));

    // Verifica se o boleto já foi gerado
    if (combinedScore.isHasBillet()) {
        throw new CombinedScoreException("O boleto para este agrupamento já foi gerado.");
    }

    try {
        // Busca o cliente associado ao agrupamento
        Client client = getClientById(combinedScore.getClientId());

        // Cria o objeto Pagador e a requisição simplificada do boleto
        Pagador pagador = billetFactory.createPagadorFromClient(client);
        BilletRequestSimplified billetRequest =
            billetFactory.createBilletRequest(combinedScore, combinedScoreId, pagador, number);

        // Emite o boleto através da API
        ResponseEntity<Map<String, Object>> billetResponse = issueBillet(billetRequest);

        // Extrai os valores retornados pela API
        Map<String, Object> responseBody = billetResponse.getBody();
        if (responseBody == null) {
            throw new CombinedScoreException("Erro ao processar a resposta da API: corpo vazio.");
        }

        byte[] pdfBytes = (byte[]) responseBody.get("pdf");
        String nossoNumero = (String) responseBody.get("nossoNumero");
        String seuNumero = (String) responseBody.get("seuNumero");

        // Atualiza o status do agrupamento para indicar que o boleto foi gerado
        combinedScore.setHasBillet(true);
        combinedScore.setOurNumber_sicoob(nossoNumero);
        combinedScore.setYourNumber(seuNumero);
        combinedScoreRepository.save(combinedScore);

        // Configura os headers para retornar o PDF como arquivo binário
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "BOL-" + combinedScore.getYourNumber() + ".pdf");

        // Retorna o PDF como resposta binária
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    } catch (Exception e) {
        throw new CombinedScoreException("Erro ao gerar o boleto: " + e.getMessage(), e);
    }
}


  /**
   * Busca o cliente pelo ID.
   *
   * @param clientId ID do cliente
   * @return Cliente encontrado
   */
  private Client getClientById(Long clientId) {
    return clientRepository
        .findById(clientId)
        .orElseThrow(() -> new BilletException("Cliente com ID " + clientId + " não encontrado."));
  }

    @Transactional
  public List<CombinedScore> syncAndFindOverdueUnpaidScores(LocalDate currentDate) {
    // Busca todos os CombinedScore vencidos e não confirmados
    List<CombinedScore> overdueScores =
        combinedScoreRepository.findOverdueUnpaidScores(currentDate);

    // Lista para armazenar os CombinedScore que permanecem pendentes
    List<CombinedScore> remainingPendingScores = new ArrayList<>(overdueScores);

    for (CombinedScore combinedScore : overdueScores) {
      // Verifica se o CombinedScore possui um boleto associado
      if (combinedScore.isHasBillet() && combinedScore.getStatus() == Status.PENDENTE) {
        try {
          // Busca a lista de boletos atualizada do BilletService
          List<BilletResponse> updatedBillets = listBilletByPayer(combinedScore.getClientId());

          // Verifica se o boleto do CombinedScore está presente na lista retornada
          boolean billetExists =
              updatedBillets.stream()
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

  /**
   * Busca boletos com filtros opcionais (nome do cliente, período, status) e paginação.
   *
   * @param name Nome do cliente (opcional).
   * @param startDate Data inicial do período (opcional).
   * @param endDate Data final do período (opcional).
   * @param status Status do boleto (opcional).
   * @param page Número da página.
   * @param size Tamanho da página.
   * @return Página contendo a lista de boletos filtrados.
   * @throws IOException Se houver erro na comunicação com a API.
   */
  public Page<BilletResponse> searchBillets(
      String name, String startDate, String endDate, String status, int page, int size)
      throws IOException {
    try {
      // Monta os parâmetros da consulta
      StringBuilder endpoint = new StringBuilder(BASE_URL + "boletos?");
      endpoint.append("numeroCliente=").append(clientNumber);

      if (name != null && !name.isBlank()) {
        endpoint.append("&nome=").append(name);
      }
      if (status != null && !status.isBlank()) {
        endpoint.append("&situacao=").append(status);
      }

      // Faz a requisição para a API
      ResponseEntity<JsonNode> response = httpClient.getWithResponse(endpoint.toString());

      // Verifica se a resposta é válida
      JsonNode resposta = response.getBody();
      if (resposta == null || resposta.isEmpty()) {
        return Page.empty();
      }

      // Acessa o campo "resultado" que contém a lista de boletos
      JsonNode resultado = resposta.path("resultado");
      if (!resultado.isArray()) {
        throw new BilletException("Resposta inválida da API: campo 'resultado' não é uma lista.");
      }

      // Mapeia os dados para uma lista de BilletResponse
      List<BilletResponse> boletos = new ArrayList<>();
      for (JsonNode boletoNode : resultado) {
        BilletResponse boleto =
            new BilletResponse(
                boletoNode.path("pagador").path("nome").asText(),
                boletoNode.path("dataEmissao").asText(),
                boletoNode.path("dataVencimento").asText(),
                boletoNode.path("seuNumero").asText(),
                boletoNode.path("situacaoBoleto").asText(),
                boletoNode.path("valor").decimalValue());
        boletos.add(boleto);
      }

      // Filtra os boletos pela data de vencimento, se os parâmetros forem fornecidos
      if (startDate != null && !startDate.isBlank()) {
        LocalDate start = LocalDate.parse(startDate);
        boletos =
            boletos.stream()
                .filter(b -> LocalDate.parse(b.dataVencimento()).isAfter(start) || LocalDate.parse(b.dataVencimento()).isEqual(start))
                .toList();
      }
      if (endDate != null && !endDate.isBlank()) {
        LocalDate end = LocalDate.parse(endDate);
        boletos =
            boletos.stream()
                .filter(b -> LocalDate.parse(b.dataVencimento()).isBefore(end) || LocalDate.parse(b.dataVencimento()).isEqual(end))
                .toList();
      }

      // Implementa a paginação manualmente
      Pageable pageable = PageRequest.of(page, size, Sort.by("dataVencimento").descending());
      int start = Math.min((int) pageable.getOffset(), boletos.size());
      int end = Math.min((start + pageable.getPageSize()), boletos.size());
      return new PageImpl<>(boletos.subList(start, end), pageable, boletos.size());

    } catch (HttpClientErrorException e) {
      throw new BilletException(
          "Erro na requisição para buscar boletos: " + e.getResponseBodyAsString(), e);
    } catch (IOException e) {
      throw new BilletException("Erro ao processar a resposta da API ao buscar boletos.", e);
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao buscar boletos.", e);
    }
  }

  /**
   * Lista o boleto específico associado a um CombinedScore.
   *
   * @param combinedScoreId ID do CombinedScore
   * @return Detalhes do boleto associado
   * @throws IOException Se houver erro na comunicação ou no processamento da resposta
   */
  public BilletResponse getBilletByCombinedScore(long combinedScoreId) throws IOException {
    // Busca o CombinedScore pelo ID
    CombinedScore combinedScore =
        combinedScoreRepository
            .findById(combinedScoreId)
            .orElseThrow(
                () ->
                    new BilletException(
                        "CombinedScore com ID " + combinedScoreId + " não encontrado."));

    // Verifica se o CombinedScore possui um número de boleto associado
    String number = combinedScore.getOurNumber_sicoob();
    if (number == null || number.isBlank()) {
      throw new BilletException("Nenhum boleto associado ao CombinedScore com ID " + combinedScoreId);
    }

    try {
      // Monta o endpoint para buscar o boleto específico
      String endpoint =
          String.format(
              BASE_URL + "boletos?numeroCliente=%d&nossoNumero=%s",
              clientNumber,
              number);

      // Faz a requisição para obter o boleto
      ResponseEntity<JsonNode> response = httpClient.getWithResponse(endpoint);

      // Verifica se a resposta é válida
      JsonNode resposta = response.getBody();
      if (resposta == null || resposta.isEmpty()) {
        throw new BilletException("Nenhum boleto encontrado para o número: " + number);
      }

      // Acessa o campo "resultado" que contém os detalhes do boleto
      JsonNode resultado = resposta.path("resultado");
      if (!resultado.isObject()) {
        throw new BilletException("Resposta inválida da API: campo 'resultado' não é um objeto.");
      }

      // Mapeia os dados para um objeto BilletResponse
      return new BilletResponse(
          resultado.path("pagador").path("nome").asText(),
          resultado.path("dataEmissao").asText(),
          resultado.path("dataVencimento").asText(),
          resultado.path("seuNumero").asText(),
          resultado.path("situacaoBoleto").asText(),
          resultado.path("valor").decimalValue());

    } catch (HttpClientErrorException e) {
      throw new BilletException(
          "Erro na requisição para buscar o boleto: " + e.getResponseBodyAsString(), e);
    } catch (IOException e) {
      throw new BilletException("Erro ao processar a resposta da API ao buscar o boleto.", e);
    } catch (Exception e) {
      throw new BilletException("Erro inesperado ao buscar o boleto.", e);
    }
  }
}