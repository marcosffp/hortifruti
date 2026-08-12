package com.hortifruti.sl.hortifruti.service.purchase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hortifruti.sl.hortifruti.dto.purchase.ItemNotaExtraido;
import com.hortifruti.sl.hortifruti.dto.purchase.NotaExtracaoResponse;
import com.hortifruti.sl.hortifruti.exception.purchase.GeminiExtractionException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * Envia a foto de uma nota de compra pro Gemini e extrai os itens em JSON estruturado, via {@code
 * response_schema} nativo da API (força o formato de saída, sem precisar parsear texto livre). A
 * imagem nunca é persistida em disco nem logada — só metadados (tamanho, tempo de resposta,
 * sucesso/falha) são registrados, para acompanhar o consumo do free tier sem expor dado de negócio
 * da loja.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiExtractionService {

  private static final String PROMPT =
      """
      Você está lendo uma foto de nota(s) manuscrita(s) de compra de hortifrúti (fornecedor → cliente).
      A imagem pode conter UMA ÚNICA nota ou VÁRIAS notas distintas fotografadas juntas (ex.: vários
      pedaços de papel separados, lado a lado ou empilhados, cada um com seu próprio cabeçalho de
      cliente/data/itens/total). Identifique cada nota como um documento distinto e devolva um item de
      lista para cada uma, na ordem em que aparecem na imagem (ex.: da esquerda pra direita, de cima
      pra baixo). NUNCA misture itens de notas diferentes dentro da mesma entrada da lista.
      Para cada nota, extraia cada linha de item com: nome do produto, quantidade, unidade (kg/un/cx
      quando aplicável), preço unitário e valor total da linha. Também extraia o nome do
      cliente/destinatário (se houver) e o total geral daquela nota.
      Se um campo estiver ilegível, retorne null nesse campo em vez de inventar um valor.
      Preserve o nome do produto como está escrito, sem corrigir ortografia.
      """;

  private static final String API_URL_TEMPLATE =
      "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

  private final NotaUploadService notaUploadService;
  private final ProdutoMatchingService produtoMatchingService;
  private final ClienteMatchingService clienteMatchingService;
  private final ObjectMapper objectMapper;

  @Qualifier("geminiRestTemplate")
  private final RestTemplate geminiRestTemplate;

  @Value("${gemini.api.key}")
  private String apiKey;

  @Value("${gemini.model}")
  private String model;

  @Value("${nota.consistencia.margem:0.05}")
  private BigDecimal margemConsistencia;

  public NotaExtracaoResponse extrair(MultipartFile file) {
    NotaUploadService.ValidatedFile validated = notaUploadService.validate(file);
    return extrairDeArquivoValidado(validated);
  }

  /**
   * Mesmo caminho de {@link #extrair}, mas pra quando os bytes já foram validados antes (ex.: a
   * captura assíncrona por dispositivo vinculado, que baixa o arquivo de volta do R2 em vez de
   * receber um {@link MultipartFile} fresco na requisição atual — ver {@code
   * CapturaExtracaoAsyncService}). Retorna só a primeira nota reconhecida na foto — usado onde só
   * faz sentido uma nota por vez (ex.: {@code /extrair}, endpoint de teste da tela de dev).
   */
  public NotaExtracaoResponse extrairDeArquivoValidado(NotaUploadService.ValidatedFile validated) {
    List<NotaExtracaoResponse> notas = extrairMultiplasDeArquivoValidado(validated);
    if (notas.isEmpty()) {
      throw new GeminiExtractionException("Nenhuma nota reconhecida na foto.");
    }
    return notas.get(0);
  }

  /**
   * Caminho principal do fluxo real de captura ({@code CapturaExtracaoAsyncService}): a foto pode
   * conter uma ou várias notas manuscritas distintas fotografadas juntas — ver o {@link #PROMPT} e
   * {@link #buildResponseSchema()}, que pedem/forçam uma lista em vez de um único objeto.
   */
  public List<NotaExtracaoResponse> extrairMultiplasDeArquivoValidado(
      NotaUploadService.ValidatedFile validated) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new GeminiExtractionException(
          "Extração automática indisponível no momento (chave da API não configurada).");
    }

    long start = System.currentTimeMillis();
    try {
      ObjectNode requestBody = buildRequestBody(validated);
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("x-goog-api-key", apiKey);
      // Corpo e resposta trafegam como String pura, nunca como JsonNode/ObjectNode: o projeto
      // roda Spring Boot 4, cujo RestTemplate usa por padrão o conversor Jackson 3
      // (tools.jackson.databind) — incompatível com o Jackson 2 clássico
      // (com.fasterxml.jackson.databind) usado no resto do backend (FocusNfeApiClient, etc.).
      // Deixar o Spring serializar/desserializar JsonNode do Jackson 2 falha silenciosamente
      // (write: ObjectNode vira bean genérico; read: "Cannot construct instance of JsonNode").
      // Serializando/parseando com o nosso próprio ObjectMapper e só trocando String com o
      // RestTemplate, esse conflito de versão nunca entra em jogo.
      String requestJson = objectMapper.writeValueAsString(requestBody);
      HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

      String url = API_URL_TEMPLATE.formatted(model);
      String responseJson = geminiRestTemplate.postForObject(url, entity, String.class);
      JsonNode responseBody = responseJson == null ? null : objectMapper.readTree(responseJson);

      List<NotaExtracaoResponse> extraction = enriquecer(parseResponse(responseBody));
      log.info(
          "Extração Gemini concluída: tamanhoBytes={}, tempoMs={}, notasEncontradas={},"
              + " sucesso=true",
          validated.bytes().length,
          System.currentTimeMillis() - start,
          extraction.size());
      return extraction;
    } catch (RestClientException | JsonProcessingException e) {
      log.warn(
          "Falha na chamada ao Gemini: tamanhoBytes={}, tempoMs={}, sucesso=false, motivo={}",
          validated.bytes().length,
          System.currentTimeMillis() - start,
          e.getMessage());
      throw new GeminiExtractionException(
          "Não foi possível extrair os dados da nota no momento. Cadastre manualmente ou tente"
              + " novamente em instantes.",
          e);
    }
  }

  private ObjectNode buildRequestBody(NotaUploadService.ValidatedFile file) {
    ObjectNode root = objectMapper.createObjectNode();
    ArrayNode contents = root.putArray("contents");
    ObjectNode content = contents.addObject();
    ArrayNode parts = content.putArray("parts");
    parts.addObject().put("text", PROMPT);

    ObjectNode inlineData = parts.addObject().putObject("inlineData");
    inlineData.put("mimeType", file.contentType());
    inlineData.put("data", Base64.getEncoder().encodeToString(file.bytes()));

    ObjectNode generationConfig = root.putObject("generationConfig");
    generationConfig.put("responseMimeType", "application/json");
    generationConfig.set("responseSchema", buildResponseSchema());
    return root;
  }

  /**
   * Raiz {@code ARRAY} — uma entrada por nota reconhecida na foto (uma única entrada no caso comum
   * de uma nota só). {@code items} é o schema de uma nota individual, ver {@link
   * #buildNotaSchema()}.
   */
  private ObjectNode buildResponseSchema() {
    ObjectNode schema = objectMapper.createObjectNode();
    schema.put("type", "ARRAY");
    schema.set("items", buildNotaSchema());
    return schema;
  }

  private ObjectNode buildNotaSchema() {
    ObjectNode schema = objectMapper.createObjectNode();
    schema.put("type", "OBJECT");

    ObjectNode properties = schema.putObject("properties");
    nullableField(properties, "cliente", "STRING");
    nullableField(properties, "data", "STRING");
    nullableField(properties, "totalGeral", "NUMBER");

    ObjectNode itens = properties.putObject("itens");
    itens.put("type", "ARRAY");
    ObjectNode item = itens.putObject("items");
    item.put("type", "OBJECT");
    ObjectNode itemProperties = item.putObject("properties");
    itemProperties.putObject("produtoLido").put("type", "STRING");
    nullableField(itemProperties, "quantidade", "NUMBER");
    nullableField(itemProperties, "unidade", "STRING");
    nullableField(itemProperties, "precoUnitario", "NUMBER");
    nullableField(itemProperties, "total", "NUMBER");
    item.putArray("required").add("produtoLido");

    schema.putArray("required").add("itens");
    return schema;
  }

  private void nullableField(ObjectNode properties, String name, String type) {
    ObjectNode field = properties.putObject(name);
    field.put("type", type);
    field.put("nullable", true);
  }

  private List<NotaExtracaoResponse> parseResponse(JsonNode body) {
    if (body == null) {
      throw new GeminiExtractionException("Resposta vazia da API do Gemini.");
    }

    JsonNode candidates = body.path("candidates");
    if (!candidates.isArray() || candidates.isEmpty()) {
      throw new GeminiExtractionException(
          "A API do Gemini não retornou nenhum candidato de extração.");
    }

    String text = candidates.get(0).path("content").path("parts").path(0).path("text").asText(null);
    if (text == null || text.isBlank()) {
      throw new GeminiExtractionException("A API do Gemini retornou uma resposta vazia.");
    }

    try {
      return objectMapper.readValue(text, new TypeReference<List<NotaExtracaoResponse>>() {});
    } catch (Exception e) {
      throw new GeminiExtractionException(
          "Não foi possível interpretar a resposta da API do Gemini.", e);
    }
  }

  private List<NotaExtracaoResponse> enriquecer(List<NotaExtracaoResponse> notas) {
    return notas.stream().map(this::enriquecerNota).toList();
  }

  /**
   * Etapas 3 (matching de produto/cliente) e 4 (checagem de consistência) da spec: pra cada item,
   * sugere o produto do catálogo mais parecido e marca a confiança; a confiança cai pra "baixa" se
   * a conta interna da linha (quantidade × preço) não bater com o total, mesmo que o produto tenha
   * sido identificado certo. No nível da nota, sugere o cliente do cadastro mais parecido com o
   * nome lido, sinaliza se a soma dos itens bate com o total geral lido, e aponta os itens de maior
   * valor pra conferir primeiro quando não bate.
   */
  private NotaExtracaoResponse enriquecerNota(NotaExtracaoResponse raw) {
    List<ItemNotaExtraido> itensEnriquecidos =
        raw.itens().stream().map(this::enriquecerItem).toList();

    BigDecimal totalCalculado = NotaConsistenciaChecker.somaItens(itensEnriquecidos);
    Boolean consistente =
        raw.totalGeral() == null
            ? null
            : NotaConsistenciaChecker.notaConsistente(
                totalCalculado, raw.totalGeral(), margemConsistencia);
    List<String> itensParaConferir =
        Boolean.FALSE.equals(consistente)
            ? NotaConsistenciaChecker.itensParaConferir(itensEnriquecidos, margemConsistencia)
            : List.of();

    ClienteMatchingService.Resultado clienteResultado =
        clienteMatchingService.buscarMelhorCandidato(raw.cliente());

    return new NotaExtracaoResponse(
        raw.cliente(),
        raw.data(),
        itensEnriquecidos,
        raw.totalGeral(),
        consistente,
        itensParaConferir,
        clienteResultado.clienteSugerido(),
        clienteResultado.confianca());
  }

  private ItemNotaExtraido enriquecerItem(ItemNotaExtraido item) {
    ProdutoMatchingService.Resultado resultado =
        produtoMatchingService.buscarMelhorCandidato(
            item.produtoLido(), item.unidade(), item.quantidade());

    String confianca = resultado.confianca();
    if (!NotaConsistenciaChecker.itemConsistente(item, margemConsistencia)) {
      confianca = "baixa";
    }

    return item.comProdutoEConfianca(resultado.produtoSugerido(), confianca);
  }
}
