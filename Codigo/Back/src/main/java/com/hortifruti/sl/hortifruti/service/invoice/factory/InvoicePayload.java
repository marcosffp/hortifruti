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

@Component
public class InvoicePayload {
  private final ObjectMapper objectMapper = new ObjectMapper();

  // Alíquotas 2026 — fase de transição (CBS 0,9% + IBS UF 0,1%)
  private static final BigDecimal CBS_ALIQUOTA        = new BigDecimal("0.009");
  private static final BigDecimal IBS_UF_ALIQUOTA     = new BigDecimal("0.001");
  private static final String     CBS_ALIQUOTA_STR    = "0.9";
  private static final String     IBS_UF_ALIQUOTA_STR = "0.1";

  private static final String IBS_CBS_SITUACAO_TRIBUTARIA      = "000";
  private static final String IBS_CBS_CLASSIFICACAO_TRIBUTARIA = "000001";

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

      // =====================================================================
      // REGRA DE ARREDONDAMENTO (Rejeição 1091 / 1092):
      //
      //   A SEFAZ valida: cbs_valor_total == soma dos cbs_valor dos itens
      //                   ibs_uf_valor_total == soma dos ibs_uf_valor dos itens
      //
      //   Estratégia adotada (v3 — corrige rejeição 1091 definitivamente):
      //
      //   O FocusNFe recebe nosso JSON e monta o XML internamente. Ao
      //   enviarmos os campos de total (cbs_valor_total, ibs_uf_valor_total,
      //   ibs_valor_total, ibs_cbs_is_valor_total), o FocusNFe os escreve no
      //   XML com sua própria escala de casas decimais — que pode diferir da
      //   soma dos itens, causando a rejeição 1091.
      //
      //   Solução: NÃO enviar os campos de total IBS/CBS.
      //   Conforme documentação do FocusNFe: "Os campos serão calculados
      //   apenas se eles não forem informados na API." O FocusNFe soma os
      //   valores dos itens e garante consistência com o XML gerado.
      //
      //   Os valores por item são calculados com 4 casas (HALF_UP),
      //   escala padrão do schema NF-e para campos de valor.
      // =====================================================================

      if (request.items() != null && !request.items().isEmpty()) {
        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal totalBase = BigDecimal.ZERO;

        for (ItemRequest item : request.items()) {
          Map<String, Object> itemMap = new HashMap<>();

          // --- campos existentes ---
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

          // --- Grupo UB: IBS/CBS por item ---
          // Calcula com 4 casas (HALF_UP) — escala padrão do schema NF-e.
          // Os totais da nota NÃO são enviados: o FocusNFe os calcula
          // somando estes valores, garantindo consistência no XML.
          BigDecimal base = toBigDecimal(item.valorBruto());
          BigDecimal cbsValor   = base.multiply(CBS_ALIQUOTA).setScale(4, RoundingMode.HALF_UP);
          BigDecimal ibsUfValor = base.multiply(IBS_UF_ALIQUOTA).setScale(4, RoundingMode.HALF_UP);

          String cbsValorStr   = cbsValor.toPlainString();
          String ibsUfValorStr = ibsUfValor.toPlainString();

          itemMap.put("ibs_cbs_situacao_tributaria",      IBS_CBS_SITUACAO_TRIBUTARIA);
          itemMap.put("ibs_cbs_classificacao_tributaria", IBS_CBS_CLASSIFICACAO_TRIBUTARIA);
          itemMap.put("ibs_cbs_base_calculo",             base);
          itemMap.put("cbs_aliquota",                     CBS_ALIQUOTA_STR);
          itemMap.put("cbs_valor",                        cbsValorStr);
          itemMap.put("ibs_uf_aliquota",                  IBS_UF_ALIQUOTA_STR);
          itemMap.put("ibs_uf_valor",                     ibsUfValorStr);
          itemMap.put("ibs_mun_aliquota",                 "0");
          itemMap.put("ibs_mun_valor",                    "0");
          itemMap.put("ibs_valor_total",                  ibsUfValorStr);

          totalBase = totalBase.add(base);
          items.add(itemMap);
        }

        payload.put("items", items);

        // Envia apenas a base de cálculo total.
        // cbs_valor_total, ibs_uf_valor_total, ibs_valor_total e
        // ibs_cbs_is_valor_total são OMITIDOS intencionalmente para que
        // o FocusNFe os calcule automaticamente a partir dos itens,
        // evitando divergência de escala entre o JSON enviado e o XML gerado.
        payload.put("ibs_cbs_base_calculo", totalBase);
      }

      if (request.informacoesAdicionaisContribuinte() != null) {
        payload.put(
            "informacoes_adicionais_contribuinte", request.informacoesAdicionaisContribuinte());
      }

      return objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      throw new InvoiceException("Erro ao construir payload", e);
    }
  }

  /**
   * Converte o valor do item (que pode vir como Double, BigDecimal ou String)
   * para BigDecimal de forma segura.
   *
   * IMPORTANTE: nunca usar BigDecimal.valueOf(double) nem new BigDecimal(double)
   * pois ambos herdam a imprecisão binária do ponto flutuante.
   * Ex: new BigDecimal(20.0598d) → 20.05979999999999971578290...
   * A única forma segura é passar pela representação String, que reflete
   * exatamente o valor decimal que o usuário informou.
   */
  private BigDecimal toBigDecimal(Object value) {
    if (value == null) return BigDecimal.ZERO;
    if (value instanceof BigDecimal) return (BigDecimal) value;
    try {
      // toString() em qualquer Number (Double, Float, Integer…) produz
      // a representação decimal canônica, sem acumular erro de IEEE 754.
      return new BigDecimal(value.toString());
    } catch (NumberFormatException e) {
      return BigDecimal.ZERO;
    }
  }
}
