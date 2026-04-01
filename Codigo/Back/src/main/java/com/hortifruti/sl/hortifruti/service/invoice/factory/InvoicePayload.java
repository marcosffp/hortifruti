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
 * <h3>Regra de cálculo — evita erros 1076 e 1080 simultaneamente</h3>
 *
 * A SEFAZ faz DUAS validações:
 *   1) ibs_cbs_base_calculo (raiz) == soma dos ibs_cbs_base_calculo (itens)
 *      E total_raiz == base_raiz × alíquota                        → erro 1076
 *   2) total_raiz == soma dos valores de imposto dos itens          → erro 1080
 *
 * <h3>Solução em 3 passos</h3>
 *
 * 1. Base de cada item = valor_bruto arredondado para 2 casas (HALF_UP)
 * 2. Valor imposto por item = base × alíquota / 100, arredondado para 2 casas
 *    → A SEFAZ compara valores com 2 casas decimais.
 * 3. Totais do raiz = soma dos valores dos itens (bottom-up)
 *    → Garante que soma(itens) == total_raiz (evita 1080).
 *    Verificação cruzada: se base_total × alíquota (2 casas) divergir,
 *    ajusta o último item para satisfazer ambas as validações.
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
      // ITENS
      // =========================
      BigDecimal somaBase        = BigDecimal.ZERO;
      BigDecimal somaCbsItens    = BigDecimal.ZERO;
      BigDecimal somaIbsUfItens  = BigDecimal.ZERO;
      BigDecimal somaIbsMunItens = BigDecimal.ZERO;

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
          //
          // ⚠️ PASSO 1: Base e valores arredondados para 2 casas decimais
          // A SEFAZ trabalha com 2 casas tanto na base quanto nos valores.
          // Se enviar com 4 casas, o FocusNFe/SEFAZ re-arredonda para 2,
          // perdendo o ajuste fino do Passo 3 → erro 1080.
          // =========================
          BigDecimal baseOriginal = item.valorBruto() != null
              ? item.valorBruto()
              : item.quantidadeComercial().multiply(item.valorUnitarioComercial());

          BigDecimal base = baseOriginal.setScale(2, RoundingMode.HALF_UP);

          // ⚠️ MUDANÇA PRINCIPAL: escala 2 (não 4) nos valores de imposto
          BigDecimal cbsValor    = base.multiply(CBS_ALIQUOTA).divide(CEM, 2, RoundingMode.HALF_UP);
          BigDecimal ibsUfValor  = base.multiply(IBS_UF_ALIQUOTA).divide(CEM, 2, RoundingMode.HALF_UP);
          BigDecimal ibsMunValor = base.multiply(IBS_MUN_ALIQUOTA).divide(CEM, 2, RoundingMode.HALF_UP);
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

          somaBase        = somaBase.add(base);
          somaCbsItens    = somaCbsItens.add(cbsValor);
          somaIbsUfItens  = somaIbsUfItens.add(ibsUfValor);
          somaIbsMunItens = somaIbsMunItens.add(ibsMunValor);

          items.add(itemMap);
        }

        // =========================================================
        // ⚠️ PASSO 2: Totais do raiz = somaBase × alíquota (2 casas)
        //
        // Calcula o valor "oficial" que a SEFAZ espera no raiz.
        // =========================================================
        BigDecimal cbsTotalRaiz    = somaBase.multiply(CBS_ALIQUOTA).divide(CEM, 2, RoundingMode.HALF_UP);
        BigDecimal ibsUfTotalRaiz  = somaBase.multiply(IBS_UF_ALIQUOTA).divide(CEM, 2, RoundingMode.HALF_UP);
        BigDecimal ibsMunTotalRaiz = somaBase.multiply(IBS_MUN_ALIQUOTA).divide(CEM, 2, RoundingMode.HALF_UP);

        // =========================================================
        // ⚠️ PASSO 3: Ajuste de arredondamento no último item
        //
        // Garante: soma(valor_itens) == total_raiz (evita 1080)
        // Com valores em 2 casas, o ajuste é ±0.01 no máximo.
        // =========================================================
        BigDecimal diffCbs    = cbsTotalRaiz.subtract(somaCbsItens);
        BigDecimal diffIbsUf  = ibsUfTotalRaiz.subtract(somaIbsUfItens);
        BigDecimal diffIbsMun = ibsMunTotalRaiz.subtract(somaIbsMunItens);

        if (diffCbs.signum() != 0 || diffIbsUf.signum() != 0 || diffIbsMun.signum() != 0) {
          Map<String, Object> ultimoItem = items.get(items.size() - 1);

          BigDecimal cbsAjustado    = ((BigDecimal) ultimoItem.get("cbs_valor")).add(diffCbs);
          BigDecimal ibsUfAjustado  = ((BigDecimal) ultimoItem.get("ibs_uf_valor")).add(diffIbsUf);
          BigDecimal ibsMunAjustado = ((BigDecimal) ultimoItem.get("ibs_mun_valor")).add(diffIbsMun);
          BigDecimal ibsTotalAjustado = ibsUfAjustado.add(ibsMunAjustado);

          ultimoItem.put("cbs_valor",       cbsAjustado);
          ultimoItem.put("ibs_uf_valor",    ibsUfAjustado);
          ultimoItem.put("ibs_mun_valor",   ibsMunAjustado);
          ultimoItem.put("ibs_valor_total", ibsTotalAjustado);
        }

        // Totais do raiz
        BigDecimal ibsTotalRaiz    = ibsUfTotalRaiz.add(ibsMunTotalRaiz);
        BigDecimal ibsCbsTotalRaiz = cbsTotalRaiz.add(ibsTotalRaiz);

        payload.put("ibs_cbs_base_calculo",   somaBase);
        payload.put("cbs_valor_total",        cbsTotalRaiz);
        payload.put("ibs_uf_valor_total",     ibsUfTotalRaiz);
        payload.put("ibs_valor_total",        ibsTotalRaiz);
        payload.put("ibs_cbs_is_valor_total", ibsCbsTotalRaiz);

        payload.put("items", items);
      }

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
