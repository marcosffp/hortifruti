package com.hortifruti.sl.hortifruti.service.invoice.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.dto.invoice.IssueInvoiceRequest;
import com.hortifruti.sl.hortifruti.dto.invoice.ItemRequest;
import com.hortifruti.sl.hortifruti.exception.InvoiceException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Responsável por montar o payload enviado para a API do FocusNFe.
 *
 * <h2>IBS/CBS — Reforma Tributária (NT 2025.002)</h2>
 *
 * Obrigatório a partir de 01/04/2026.
 * Alíquotas vigentes em 2026:
 * - CBS (federal): 0,9%
 * - IBS UF (estadual): 0,1%
 * - IBS Mun (municipal): 0,0%
 *
 * <h3>Regra de cálculo — evita erros 1076 e 1080</h3>
 *
 * A SEFAZ valida DUAS coisas:
 *   1) ibs_cbs_base_calculo (raiz) == soma dos ibs_cbs_base_calculo (itens)  → erro 1076
 *   2) ibs_uf_valor_total   (raiz) == soma dos ibs_uf_valor         (itens)  → erro 1080
 *      cbs_valor_total      (raiz) == soma dos cbs_valor             (itens)  → idem
 *
 * Portanto TODOS os campos do nível raiz devem ser a SOMA LITERAL
 * dos valores já calculados por item. Nunca recalcular sobre a base total.
 *
 * <h3>Fluxo por item</h3>
 *   1. base = valor_bruto arredondado para 2 casas (HALF_UP)
 *   2. cbs_valor    = base × 0,9 / 100  → 4 casas (HALF_UP)
 *   3. ibs_uf_valor = base × 0,1 / 100  → 4 casas (HALF_UP)
 *   4. ibs_mun_valor = base × 0,0 / 100 → 4 casas (HALF_UP)
 *
 * <h3>Totais do raiz</h3>
 *   ibs_cbs_base_calculo  = Σ base(item)
 *   cbs_valor_total       = Σ cbs_valor(item)
 *   ibs_uf_valor_total    = Σ ibs_uf_valor(item)
 *   ibs_valor_total       = Σ ibs_valor_total(item)   (= ibs_uf + ibs_mun)
 *   ibs_cbs_is_valor_total = cbs_valor_total + ibs_valor_total
 */
@Component
public class InvoicePayload {

  private static final BigDecimal CBS_ALIQUOTA     = new BigDecimal("0.9");
  private static final BigDecimal IBS_UF_ALIQUOTA  = new BigDecimal("0.1");
  private static final BigDecimal IBS_MUN_ALIQUOTA = BigDecimal.ZERO;
  private static final BigDecimal CEM              = new BigDecimal("100");

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${focus.nfe.token}")
  private String focusNfeToken;

  @Value("${focus.nfe.api.url}")
  private String focusNfeApiUrl;

  @Value("${focus.nfe.cnpj.emitente}")
  private String focusNfeCnpjEmitente;

  public String buildFocusNfePayload(IssueInvoiceRequest request, String ref) {
    try {
      Map<String, Object> payload = new HashMap<>();

      // =========================
      // DADOS GERAIS
      // =========================
      payload.put("natureza_operacao", request.naturezaOperacao());
      payload.put("data_emissao", request.dataEmissao());
      payload.put("tipo_documento", "1");
      payload.put("finalidade_emissao", "1");
      payload.put("presenca_comprador", "9");

      if (focusNfeCnpjEmitente != null && !focusNfeCnpjEmitente.trim().isEmpty()) {
        payload.put("cnpj_emitente", focusNfeCnpjEmitente.trim());
      }

      // =========================
      // DESTINATÁRIO
      // =========================
      if (request.destinatario() != null) {

        if (request.destinatario().cpf() != null) {
          payload.put("cpf_destinatario", request.destinatario().cpf());
        }

        if (request.destinatario().cnpj() != null) {
          payload.put("cnpj_destinatario", request.destinatario().cnpj());
        }

        payload.put("nome_destinatario",     request.destinatario().nome());
        payload.put("telefone_destinatario", request.destinatario().telefone());
        payload.put("email_destinatario",    request.destinatario().email());

        payload.put(
            "indicador_inscricao_estadual_destinatario",
            request.destinatario().indicadorInscricaoEstadual());

        if (request.destinatario().inscricaoEstadual() != null) {
          payload.put(
              "inscricao_estadual_destinatario",
              request.destinatario().inscricaoEstadual());
        }

        if (request.destinatario().endereco() != null) {
          payload.put("logradouro_destinatario", request.destinatario().endereco().logradouro());
          payload.put("numero_destinatario",     request.destinatario().endereco().numero());

          if (request.destinatario().endereco().complemento() != null) {
            payload.put(
                "complemento_destinatario",
                request.destinatario().endereco().complemento());
          }

          payload.put("bairro_destinatario",    request.destinatario().endereco().bairro());
          payload.put("municipio_destinatario", request.destinatario().endereco().municipio());
          payload.put("uf_destinatario",        request.destinatario().endereco().uf());
          payload.put("cep_destinatario",       request.destinatario().endereco().cep());

          if (request.destinatario().endereco().codigoMunicipio() != null) {
            payload.put(
                "codigo_municipio_destinatario",
                request.destinatario().endereco().codigoMunicipio());
          }

          payload.put(
              "pais_destinatario",
              request.destinatario().endereco().nomePais() != null
                  ? request.destinatario().endereco().nomePais()
                  : "Brasil");

          payload.put(
              "codigo_pais_destinatario",
              request.destinatario().endereco().codigoPais() != null
                  ? request.destinatario().endereco().codigoPais()
                  : "1058");
        }
      }

      // =========================
      // FRETE
      // =========================
      payload.put("modalidade_frete", "9");

      // =========================
      // ITENS + ACUMULADORES PARA TOTAIS DO RAIZ
      // =========================
      //
      // ⚠️ REGRA CRÍTICA — EVITA ERROS 1076 E 1080:
      //
      // Todos os totais do nível raiz DEVEM ser a soma literal
      // dos valores calculados por item. Nunca recalcular aplicando
      // alíquota sobre a base total — a acumulação de arredondamentos
      // gera divergência.
      // =========================

      BigDecimal somaBase       = BigDecimal.ZERO;
      BigDecimal somaCbsValor   = BigDecimal.ZERO;
      BigDecimal somaIbsUfValor = BigDecimal.ZERO;
      BigDecimal somaIbsMunValor = BigDecimal.ZERO;

      if (request.items() != null && !request.items().isEmpty()) {
        List<Map<String, Object>> items = new ArrayList<>();

        for (ItemRequest item : request.items()) {
          Map<String, Object> itemMap = new HashMap<>();

          itemMap.put("numero_item",             items.size() + 1);
          itemMap.put("codigo_produto",           item.codigoProduto());
          itemMap.put("descricao",                item.descricao());
          itemMap.put("codigo_ncm",               item.ncm());
          itemMap.put("cfop",                     item.cfop());

          itemMap.put("unidade_comercial",        item.unidadeComercial());
          itemMap.put("quantidade_comercial",     item.quantidadeComercial());
          itemMap.put("valor_unitario_comercial", item.valorUnitarioComercial());
          itemMap.put("valor_bruto",              item.valorBruto());

          // Tributável (fallback automático)
          itemMap.put(
              "unidade_tributavel",
              item.unidadeTributavel() != null
                  ? item.unidadeTributavel()
                  : item.unidadeComercial());

          itemMap.put(
              "quantidade_tributavel",
              item.quantidadeTributavel() != null
                  ? item.quantidadeTributavel()
                  : item.quantidadeComercial());

          itemMap.put(
              "valor_unitario_tributavel",
              item.valorUnitarioTributavel() != null
                  ? item.valorUnitarioTributavel()
                  : item.valorUnitarioComercial());

          itemMap.put("icms_situacao_tributaria",   item.icmsSituacaoTributaria());
          itemMap.put("icms_origem",                item.icmsOrigem());
          itemMap.put("pis_situacao_tributaria",    item.pisSituacaoTributaria());
          itemMap.put("cofins_situacao_tributaria", item.cofinsSituacaoTributaria());

          // =========================
          // IBS / CBS por item
          // Base: arredondada para 2 casas (HALF_UP)
          // Valores: 4 casas decimais (HALF_UP)
          // =========================
          BigDecimal base = item.valorBruto() != null
              ? item.valorBruto()
              : item.quantidadeComercial().multiply(item.valorUnitarioComercial());
          base = base.setScale(2, RoundingMode.HALF_UP);

          BigDecimal cbsValor    = base.multiply(CBS_ALIQUOTA).divide(CEM, 4, RoundingMode.HALF_UP);
          BigDecimal ibsUfValor  = base.multiply(IBS_UF_ALIQUOTA).divide(CEM, 4, RoundingMode.HALF_UP);
          BigDecimal ibsMunValor = base.multiply(IBS_MUN_ALIQUOTA).divide(CEM, 4, RoundingMode.HALF_UP);
          BigDecimal ibsValorTotal = ibsUfValor.add(ibsMunValor);

          itemMap.put("ibs_cbs_situacao_tributaria",      "000");
          itemMap.put("ibs_cbs_classificacao_tributaria", "000001");
          itemMap.put("ibs_cbs_base_calculo",             base);
          itemMap.put("cbs_aliquota",                     CBS_ALIQUOTA);
          itemMap.put("cbs_valor",                        cbsValor);
          itemMap.put("ibs_uf_aliquota",                  IBS_UF_ALIQUOTA);
          itemMap.put("ibs_uf_valor",                     ibsUfValor);
          itemMap.put("ibs_mun_aliquota",                 IBS_MUN_ALIQUOTA);
          itemMap.put("ibs_mun_valor",                    ibsMunValor);
          itemMap.put("ibs_valor_total",                  ibsValorTotal);

          // ⚠️ Acumula TODOS os valores para os totais do raiz
          somaBase        = somaBase.add(base);
          somaCbsValor    = somaCbsValor.add(cbsValor);
          somaIbsUfValor  = somaIbsUfValor.add(ibsUfValor);
          somaIbsMunValor = somaIbsMunValor.add(ibsMunValor);

          items.add(itemMap);
        }

        payload.put("items", items);
      }

      // =========================
      // TOTAIS IBS/CBS — NÍVEL RAIZ
      //
      // Todos os campos são SOMA DIRETA dos valores dos itens.
      // Isso garante consistência com a validação da SEFAZ que
      // compara raiz vs soma dos itens (erros 1076 e 1080).
      // =========================
      BigDecimal somaIbsTotal    = somaIbsUfValor.add(somaIbsMunValor);
      BigDecimal somaIbsCbsTotal = somaCbsValor.add(somaIbsTotal);

      payload.put("ibs_cbs_base_calculo",   somaBase);
      payload.put("cbs_valor_total",        somaCbsValor);
      payload.put("ibs_uf_valor_total",     somaIbsUfValor);
      payload.put("ibs_valor_total",        somaIbsTotal);
      payload.put("ibs_cbs_is_valor_total", somaIbsCbsTotal);

      // =========================
      // INFORMAÇÕES ADICIONAIS
      // =========================
      if (request.informacoesAdicionaisContribuinte() != null) {
        payload.put(
            "informacoes_adicionais_contribuinte",
            request.informacoesAdicionaisContribuinte());
      }

      return objectMapper.writeValueAsString(payload);

    } catch (Exception e) {
      throw new InvoiceException("Erro ao construir payload da NFe", e);
    }
  }
}
