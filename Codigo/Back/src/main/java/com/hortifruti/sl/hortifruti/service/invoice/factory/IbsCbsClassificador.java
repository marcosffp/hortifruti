package com.hortifruti.sl.hortifruti.service.invoice.factory;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Classifica a tributação de IBS/CBS de um item pelo NCM, conforme a Tabela de Classificação
 * Tributária (cClassTrib) da LC 214/2025 publicada no Portal dos Documentos Fiscais Eletrônicos -
 * SVRS (dfe-portal.svrs.rs.gov.br), que é a base que alimenta a validação da Sefaz.
 *
 * <p>Cada benefício fiscal (redução de alíquota associada a um cClassTrib) é uma entrada em {@link
 * #CATEGORIAS_COM_BENEFICIO_FISCAL}, identificada pelos prefixos de NCM aos quais se aplica. Um
 * item cujo NCM não bate com nenhuma entrada cai no padrão de tributação integral (CST 000 /
 * cClassTrib 000001, sem redução). Para adicionar uma nova categoria (ex.: cesta básica nacional de
 * alimentos — cClassTrib 200013 — para arroz, feijão etc.), basta acrescentar uma entrada na lista
 * com o CST, o cClassTrib, o percentual de redução (tags Sefaz {@code pRedIBS}/{@code pRedCBS} do
 * cClassTrib) e os prefixos de NCM cobertos; não é preciso alterar {@link #classificar}. As
 * entradas são avaliadas na ordem declarada — a primeira cujo prefixo bater com o NCM é usada.
 *
 * <p>Exemplo já validado: produtos hortícolas, frutas e ovos (NCM capítulos 07, 08 e 0407) têm
 * redução de 100% na alíquota de IBS/CBS por força do Anexo XV da LC 214/2025 (art. 148/224) — CST
 * 200 / cClassTrib 200014. Percentual confirmado na base do Portal SVRS: {@code
 * CodClassTrib=200014, PercRedIbs=100, PercRedCbs=100, NroAnexo=15}. Um percentual de redução que
 * não bata com o cadastrado na Sefaz para aquele cClassTrib causa a rejeição 1034 ("Percentual de
 * reducao de aliquota da UF nao e valido para este cClassTrib").
 *
 * <p>Importante: o percentual de redução de uma categoria NÃO é aplicado zerando (ou reduzindo) a
 * alíquota nominal de CBS/IBS UF enviada à Sefaz. Durante a fase de testes de 2026 (NT 2025.002
 * v1.20), a alíquota nominal (tags {@code pCBS}/{@code pIBSUF}) é fixa em 0,9%/0,1% para TODOS os
 * itens, inclusive os com redução — alterá-la causa a rejeição Sefaz 1026 ("Aliquota do IBS da UF
 * invalida"). A redução é informada à parte (tag {@code pRedAliq}), a partir da qual o consumidor
 * desta classe (ver {@link InvoicePayload}) calcula a alíquota efetiva (tag {@code pAliqEfet} =
 * nominal × (1 − redução)) que de fato é usada para calcular o valor do imposto.
 */
@Component
public class IbsCbsClassificador {

  private static final String CST_TRIBUTACAO_INTEGRAL = "000";
  private static final String CLASSIFICACAO_TRIBUTACAO_INTEGRAL = "000001";

  private static final IbsCbsClassificacao TRIBUTACAO_INTEGRAL =
      new IbsCbsClassificacao(
          CST_TRIBUTACAO_INTEGRAL, CLASSIFICACAO_TRIBUTACAO_INTEGRAL, BigDecimal.ZERO);

  /**
   * Tabela de benefícios fiscais de IBS/CBS por NCM. Para adicionar uma categoria nova, acrescente
   * uma entrada aqui — não mexa em {@link #classificar}.
   */
  private static final List<CategoriaFiscal> CATEGORIAS_COM_BENEFICIO_FISCAL =
      List.of(
          new CategoriaFiscal(
              "200",
              "200014",
              new BigDecimal("100"), // Anexo XV LC 214/2025 — PercRedIbs/PercRedCbs = 100
              List.of("07", "08", "0407") // hortícolas, frutas, ovos
              ));

  public IbsCbsClassificacao classificar(String ncm) {
    return CATEGORIAS_COM_BENEFICIO_FISCAL.stream()
        .filter(categoria -> categoria.aplicavelA(ncm))
        .findFirst()
        .map(CategoriaFiscal::classificacao)
        .orElse(TRIBUTACAO_INTEGRAL);
  }

  /**
   * Um benefício fiscal de IBS/CBS: o CST e o cClassTrib que o identificam, o percentual de redução
   * de alíquota correspondente (tags Sefaz {@code pRedIBS}/{@code pRedCBS} do cClassTrib) e os
   * prefixos de NCM aos quais ele se aplica.
   */
  private record CategoriaFiscal(
      String cst,
      String cClassTrib,
      BigDecimal percentualReducaoAliquota,
      List<String> prefixosNcm) {

    boolean aplicavelA(String ncm) {
      return ncm != null && prefixosNcm.stream().anyMatch(ncm::startsWith);
    }

    IbsCbsClassificacao classificacao() {
      return new IbsCbsClassificacao(cst, cClassTrib, percentualReducaoAliquota);
    }
  }
}
