package com.hortifruti.sl.hortifruti.service.invoice.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.dto.invoice.IssueInvoiceRequest;
import com.hortifruti.sl.hortifruti.dto.invoice.ItemRequest;
import com.hortifruti.sl.hortifruti.exception.invoice.InvoiceException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Responsável por montar o payload enviado para a API do FocusNFe.
 *
 * <h2>IBS/CBS — Reforma Tributária (NT 2025.002)</h2>
 *
 * Obrigatório a partir de 01/04/2026. Alíquotas NOMINAIS vigentes em 2026 (fase de testes, NT
 * 2025.002 v1.20): CBS = 0,9%, IBS UF = 0,1%, IBS Mun = 0,0% — fixas para TODOS os itens,
 * independentemente de classificação. Enviar uma alíquota nominal diferente (inclusive zero, para
 * itens com benefício) causa a rejeição Sefaz 1026 ("Aliquota do IBS da UF invalida").
 *
 * <p>{@link IbsCbsClassificador} decide, a partir do NCM, apenas o CST/cClassTrib e o percentual de
 * redução do item (ex.: produtos hortícolas, frutas e ovos — Anexo II da LC 214/2025 — têm redução
 * de 60%). Esse percentual é aplicado sobre a alíquota nominal para obter a alíquota EFETIVA (tag
 * Sefaz {@code pAliqEfet} = nominal × (1 − redução)), que é o que efetivamente entra no cálculo do
 * valor do imposto do item — a alíquota nominal enviada continua sempre a mesma.
 *
 * <h3>Cálculo dos totais do raiz</h3>
 *
 * Cada item já é arredondado para 2 casas (HALF_UP) individualmente, e os totais do raiz são a soma
 * direta dos valores já arredondados dos itens. Isso satisfaz por construção a validação da SEFAZ
 * de total_raiz == soma dos valores dos itens (erro 1080), sem precisar de redistribuição de
 * centavos, inclusive quando a nota mistura itens com reduções diferentes.
 */
@Component
@RequiredArgsConstructor
public class InvoicePayload {

  private final IbsCbsClassificador ibsCbsClassificador;

  private static final BigDecimal CBS_ALIQUOTA = new BigDecimal("0.9");
  private static final BigDecimal IBS_UF_ALIQUOTA = new BigDecimal("0.1");
  private static final BigDecimal IBS_MUN_ALIQUOTA = BigDecimal.ZERO;
  private static final BigDecimal CEM = new BigDecimal("100");

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

      payload.put("natureza_operacao", request.naturezaOperacao());
      payload.put("data_emissao", request.dataEmissao());
      payload.put("tipo_documento", "1");
      payload.put("finalidade_emissao", "1");
      payload.put("presenca_comprador", "9");

      if (focusNfeCnpjEmitente != null && !focusNfeCnpjEmitente.trim().isEmpty()) {
        payload.put("cnpj_emitente", focusNfeCnpjEmitente.trim());
      }

      if (request.destinatario() != null) {

        if (request.destinatario().cpf() != null) {
          payload.put("cpf_destinatario", request.destinatario().cpf());
        }

        if (request.destinatario().cnpj() != null) {
          payload.put("cnpj_destinatario", request.destinatario().cnpj());
        }

        payload.put("nome_destinatario", request.destinatario().nome());
        payload.put("telefone_destinatario", request.destinatario().telefone());
        payload.put("email_destinatario", request.destinatario().email());

        payload.put(
            "indicador_inscricao_estadual_destinatario",
            request.destinatario().indicadorInscricaoEstadual());

        if (request.destinatario().inscricaoEstadual() != null) {
          payload.put(
              "inscricao_estadual_destinatario", request.destinatario().inscricaoEstadual());
        }

        if (request.destinatario().endereco() != null) {
          payload.put("logradouro_destinatario", request.destinatario().endereco().logradouro());
          payload.put("numero_destinatario", request.destinatario().endereco().numero());

          if (request.destinatario().endereco().complemento() != null) {
            payload.put(
                "complemento_destinatario", request.destinatario().endereco().complemento());
          }

          payload.put("bairro_destinatario", request.destinatario().endereco().bairro());
          payload.put("municipio_destinatario", request.destinatario().endereco().municipio());
          payload.put("uf_destinatario", request.destinatario().endereco().uf());
          payload.put("cep_destinatario", request.destinatario().endereco().cep());

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

      payload.put("modalidade_frete", "9");

      BigDecimal somaBase = BigDecimal.ZERO;
      BigDecimal somaCbsItens = BigDecimal.ZERO;
      BigDecimal somaIbsUfItens = BigDecimal.ZERO;
      BigDecimal somaIbsMunItens = BigDecimal.ZERO;

      if (request.items() != null && !request.items().isEmpty()) {
        List<Map<String, Object>> items = new ArrayList<>();

        for (ItemRequest item : request.items()) {
          Map<String, Object> itemMap = new HashMap<>();

          itemMap.put("numero_item", items.size() + 1);
          itemMap.put("codigo_produto", item.codigoProduto());
          itemMap.put("descricao", item.descricao());
          itemMap.put("codigo_ncm", item.ncm());
          itemMap.put("cfop", item.cfop());

          itemMap.put("unidade_comercial", item.unidadeComercial());
          itemMap.put("quantidade_comercial", item.quantidadeComercial());
          itemMap.put("valor_unitario_comercial", item.valorUnitarioComercial());
          itemMap.put("valor_bruto", item.valorBruto());

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

          itemMap.put("icms_situacao_tributaria", item.icmsSituacaoTributaria());
          itemMap.put("icms_origem", item.icmsOrigem());
          itemMap.put("pis_situacao_tributaria", item.pisSituacaoTributaria());
          itemMap.put("cofins_situacao_tributaria", item.cofinsSituacaoTributaria());

          // =========================
          // IBS / CBS por item
          //
          // Base arredondada para 2 casas decimais (a SEFAZ trabalha com 2 casas tanto na base
          // quanto nos valores). CST/cClassTrib e percentual de redução são decididos por item, a
          // partir do NCM; as alíquotas nominais (cbs_aliquota/ibs_uf_aliquota/ibs_mun_aliquota)
          // são sempre as mesmas — é a alíquota EFETIVA (nominal × (1 − redução)) que varia por
          // item e que entra no cálculo do valor do imposto.
          // =========================
          BigDecimal baseOriginal =
              item.valorBruto() != null
                  ? item.valorBruto()
                  : item.quantidadeComercial().multiply(item.valorUnitarioComercial());

          BigDecimal base = baseOriginal.setScale(2, RoundingMode.HALF_UP);

          IbsCbsClassificacao classificacao = ibsCbsClassificador.classificar(item.ncm());
          BigDecimal percentualReducao = classificacao.percentualReducaoAliquota();
          BigDecimal fatorEfetivo =
              CEM.subtract(percentualReducao).divide(CEM, 4, RoundingMode.HALF_UP);

          BigDecimal cbsAliquotaEfetiva =
              CBS_ALIQUOTA.multiply(fatorEfetivo).setScale(4, RoundingMode.HALF_UP);
          BigDecimal ibsUfAliquotaEfetiva =
              IBS_UF_ALIQUOTA.multiply(fatorEfetivo).setScale(4, RoundingMode.HALF_UP);
          BigDecimal ibsMunAliquotaEfetiva =
              IBS_MUN_ALIQUOTA.multiply(fatorEfetivo).setScale(4, RoundingMode.HALF_UP);

          BigDecimal cbsValor =
              base.multiply(cbsAliquotaEfetiva).divide(CEM, 2, RoundingMode.HALF_UP);
          BigDecimal ibsUfValor =
              base.multiply(ibsUfAliquotaEfetiva).divide(CEM, 2, RoundingMode.HALF_UP);
          BigDecimal ibsMunValor =
              base.multiply(ibsMunAliquotaEfetiva).divide(CEM, 2, RoundingMode.HALF_UP);
          BigDecimal ibsValorTotal = ibsUfValor.add(ibsMunValor);

          itemMap.put("ibs_cbs_situacao_tributaria", classificacao.situacaoTributaria());
          itemMap.put("ibs_cbs_classificacao_tributaria", classificacao.classificacaoTributaria());
          itemMap.put("ibs_cbs_base_calculo", base);
          itemMap.put("cbs_aliquota", CBS_ALIQUOTA);
          itemMap.put("cbs_valor", cbsValor);
          itemMap.put("ibs_uf_aliquota", IBS_UF_ALIQUOTA);
          itemMap.put("ibs_uf_valor", ibsUfValor);
          itemMap.put("ibs_mun_aliquota", IBS_MUN_ALIQUOTA);
          itemMap.put("ibs_mun_valor", ibsMunValor);
          itemMap.put("ibs_valor_total", ibsValorTotal);

          if (percentualReducao.signum() > 0) {
            itemMap.put("cbs_percentual_reducao_aliquota", percentualReducao);
            itemMap.put("cbs_aliquota_efetiva", cbsAliquotaEfetiva);
            itemMap.put("ibs_uf_percentual_reducao_aliquota", percentualReducao);
            itemMap.put("ibs_uf_aliquota_efetiva", ibsUfAliquotaEfetiva);
            itemMap.put("ibs_mun_percentual_reducao_aliquota", percentualReducao);
            itemMap.put("ibs_mun_aliquota_efetiva", ibsMunAliquotaEfetiva);
          }

          somaBase = somaBase.add(base);
          somaCbsItens = somaCbsItens.add(cbsValor);
          somaIbsUfItens = somaIbsUfItens.add(ibsUfValor);
          somaIbsMunItens = somaIbsMunItens.add(ibsMunValor);

          items.add(itemMap);
        }

        // =========================================================
        // Totais do raiz = soma direta dos valores já arredondados dos itens.
        //
        // Como cada item já está arredondado para 2 casas individualmente, a soma de valores
        // com escala 2 é exata — não há arredondamento residual a redistribuir. Isso garante
        // por construção soma(valor_itens) == total_raiz (evita erro 1080), inclusive quando a
        // nota mistura itens com alíquotas diferentes (ex.: hortifrúti a zero + demais produtos
        // na alíquota padrão).
        // =========================================================
        BigDecimal cbsTotalRaiz = somaCbsItens;
        BigDecimal ibsUfTotalRaiz = somaIbsUfItens;
        BigDecimal ibsMunTotalRaiz = somaIbsMunItens;

        BigDecimal ibsTotalRaiz = ibsUfTotalRaiz.add(ibsMunTotalRaiz);
        BigDecimal ibsCbsTotalRaiz = cbsTotalRaiz.add(ibsTotalRaiz);

        payload.put("ibs_cbs_base_calculo", somaBase);
        payload.put("cbs_valor_total", cbsTotalRaiz);
        payload.put("ibs_uf_valor_total", ibsUfTotalRaiz);
        payload.put("ibs_valor_total", ibsTotalRaiz);
        payload.put("ibs_cbs_is_valor_total", ibsCbsTotalRaiz);

        payload.put("items", items);
      }

      if (request.informacoesAdicionaisContribuinte() != null) {
        payload.put(
            "informacoes_adicionais_contribuinte", request.informacoesAdicionaisContribuinte());
      }

      return objectMapper.writeValueAsString(payload);

    } catch (Exception e) {
      throw new InvoiceException("Erro ao construir payload da NFe", e);
    }
  }
}
