package com.hortifruti.sl.hortifruti.service.purchase;

import com.hortifruti.sl.hortifruti.dto.purchase.ClienteSugerido;
import com.hortifruti.sl.hortifruti.model.purchase.Client;
import com.hortifruti.sl.hortifruti.repository.purchase.ClientRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Cruza o {@code cliente} lido pela OCR (geralmente incompleto — só a parte mais reconhecível do
 * nome — e às vezes com erro de digitação/leitura, ex.: "Lotacaoo" por "Lotação") com o cadastro,
 * pra sugerir o cliente certo em vez do usuário ter que digitar de novo algo que a tela já sabia
 * (mesma ideia do {@link ProdutoMatchingService}, mas sem a complexidade de tokens de unidade —
 * nomes de cliente não têm o mesmo problema de variantes "vizinhas" colidindo por similaridade de
 * texto que o catálogo de produtos tem).
 */
@Service
@RequiredArgsConstructor
public class ClienteMatchingService {

  private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
  private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

  private static final double FUZZY_TOKEN_RATIO_LIMIT = 0.34;
  private static final double COBERTURA_TOKEN_TETO = 0.9;
  private static final int SUBSTRING_TAMANHO_MINIMO = 3;

  private final ClientRepository clientRepository;

  // Default em Java (não só no application.properties) pra continuar correto quando a classe é
  // instanciada direto em teste unitário, sem contexto Spring processando o @Value.
  @Value("${nota.matching.cliente.limiar:0.55}")
  private double limiar = 0.55;

  @Value("${nota.matching.cliente.confianca-alta:0.85}")
  private double confiancaAltaLimiar = 0.85;

  public record Resultado(ClienteSugerido clienteSugerido, String confianca) {}

  public Resultado buscarMelhorCandidato(String clienteLido) {
    return buscarMelhorCandidato(clienteLido, clientRepository.findAll());
  }

  /**
   * Overload que recebe o cadastro direto, sem tocar no banco — pra dar pra testar a lógica de
   * score isoladamente.
   */
  public Resultado buscarMelhorCandidato(String clienteLido, List<Client> clientes) {
    if (clienteLido == null || clienteLido.isBlank() || clientes.isEmpty()) {
      return new Resultado(null, "baixa");
    }

    String normLido = normalize(clienteLido);
    if (normLido.isBlank()) {
      return new Resultado(null, "baixa");
    }
    Set<String> tokensLido = tokenize(normLido);

    Client melhor = null;
    double melhorScore = -1;
    for (Client cliente : clientes) {
      double score = score(normLido, tokensLido, cliente);
      if (score > melhorScore) {
        melhorScore = score;
        melhor = cliente;
      }
    }

    if (melhor == null || melhorScore < limiar) {
      return new Resultado(null, "baixa");
    }

    String confianca = melhorScore >= confiancaAltaLimiar ? "alta" : "media";
    ClienteSugerido sugestao =
        new ClienteSugerido(melhor.getId(), melhor.getClientName(), round(melhorScore));
    return new Resultado(sugestao, confianca);
  }

  private double score(String normLido, Set<String> tokensLido, Client cliente) {
    String normNome = normalize(cliente.getClientName());
    if (normLido.equals(normNome)) {
      return 1.0;
    }

    double melhor = 0;

    // Substring bruto (nas duas direções) — sinal forte pro caso comum: OCR lê só um pedaço do
    // nome (ex.: "IRANI" de "IRANI PAPEL E EMBALAGENS S A"), ou lê o nome inteiro com um caractere
    // a mais/a menos (ex.: "LOTACAOO" contém "LOTACAO").
    if (normLido.length() >= SUBSTRING_TAMANHO_MINIMO
        && (normNome.contains(normLido) || normLido.contains(normNome))) {
      double proporcaoTamanho =
          (double) Math.min(normLido.length(), normNome.length())
              / Math.max(normLido.length(), normNome.length());
      melhor = Math.max(melhor, 0.85 + 0.15 * proporcaoTamanho);
    }

    // Cobertura por token, tolerante a erro de digitação no meio da palavra (ex.: "lotecao" ~
    // "lotacao"), pro caso em que o substring bruto não bate.
    if (!tokensLido.isEmpty()) {
      Set<String> tokensNome = tokenize(normNome);
      long tokensCasados =
          tokensLido.stream().filter(t -> temCorrespondenciaFuzzy(t, tokensNome)).count();
      double cobertura = (double) tokensCasados / tokensLido.size();
      melhor = Math.max(melhor, cobertura * COBERTURA_TOKEN_TETO);
    }

    return Math.min(1.0, melhor);
  }

  private boolean temCorrespondenciaFuzzy(String token, Set<String> candidatos) {
    return candidatos.stream().anyMatch(candidato -> tokensEquivalentes(token, candidato));
  }

  private boolean tokensEquivalentes(String a, String b) {
    if (a.equals(b)) {
      return true;
    }
    if (a.length() >= 3 && b.length() >= 3 && (a.startsWith(b) || b.startsWith(a))) {
      return true;
    }
    return levenshteinRatio(a, b) <= FUZZY_TOKEN_RATIO_LIMIT;
  }

  private String normalize(String text) {
    String semAcento =
        DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
    return semAcento.toLowerCase(Locale.ROOT).trim();
  }

  private Set<String> tokenize(String normalized) {
    return NON_ALNUM
        .splitAsStream(normalized)
        .filter(t -> !t.isBlank())
        .collect(Collectors.toSet());
  }

  private int levenshtein(String a, String b) {
    int m = a.length();
    int n = b.length();
    if (m == 0) {
      return n;
    }
    if (n == 0) {
      return m;
    }

    int[] dp = new int[n + 1];
    for (int j = 0; j <= n; j++) {
      dp[j] = j;
    }
    for (int i = 1; i <= m; i++) {
      int prev = dp[0];
      dp[0] = i;
      for (int j = 1; j <= n; j++) {
        int temp = dp[j];
        dp[j] =
            a.charAt(i - 1) == b.charAt(j - 1)
                ? prev
                : 1 + Math.min(prev, Math.min(dp[j], dp[j - 1]));
        prev = temp;
      }
    }
    return dp[n];
  }

  private double levenshteinRatio(String a, String b) {
    int maxLen = Math.max(a.length(), b.length());
    if (maxLen == 0) {
      return 0;
    }
    return (double) levenshtein(a, b) / maxLen;
  }

  private double round(double value) {
    return Math.round(value * 100) / 100.0;
  }
}
