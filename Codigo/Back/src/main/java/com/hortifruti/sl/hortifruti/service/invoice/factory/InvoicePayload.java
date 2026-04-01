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
 * <h2>⚠️ PROBLEMA IMPORTANTE (IBS/CBS)</h2>
 *
 * A SEFAZ rejeita a nota quando há divergência entre:
 * - o total informado no XML
 * - a soma dos valores dos itens
 *
 * Isso ocorre porque:
 * - O FocusNFe escreve alguns totais com 2 casas decimais
 * - Os itens podem estar com 4 casas decimais
 * - A SEFAZ valida a soma exata → qualquer diferença gera rejeição
 *
 * <h3>🚨 REGRA CRÍTICA</h3>
 *
 * NÃO enviar os seguintes campos no nível raiz:
 *
 * - ibs_cbs_base_calculo
 * - cbs_valor_total
 * - ibs_uf_valor_total
 * - ibs_valor_total
 * - ibs_cbs_is_valor_total
 *
 * Esses valores DEVEM ser calculados pelo próprio FocusNFe.
 *
 * <h3>✅ SOLUÇÃO ADOTADA</h3>
 *
 * - Nunca adicionar esses campos manualmente no nível raiz
 * - Remover explicitamente do payload (fail-safe)
 * - Calcular CBS e IBS por item com 4 casas decimais (HALF_UP)
 *
 * Isso garante consistência entre:
 * - XML gerado
 * - validação da SEFAZ
 *
 * <h3>📋 REFORMA TRIBUTÁRIA (obrigatório a partir de 01/04/2026)</h3>
 *
 * Campos incluídos por item:
 * - ibs_cbs_situacao_tributaria  → "000" (tributado normalmente)
 * - ibs_cbs_classificacao_tributaria → "000001"
 * - ibs_cbs_base_calculo         → valor bruto do item
 * - cbs_aliquota / cbs_valor     → 0,9% federal
 * - ibs_uf_aliquota / ibs_uf_valor → 0,1% estadual
 * - ibs_mun_aliquota / ibs_mun_valor → 0,0% municipal (sem alíquota definida)
 * - ibs_valor_total              → soma ibs_uf + ibs_mun
 */
@Component
public class InvoicePayload {

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

        payload.put("nome_destinatario", request.destinatario().nome());
        payload.put("telefone_destinatario", request.destinatario().telefone());
        payload.put("email_destinatario", request.destinatario().email());

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
          payload.put("numero_destinatario", request.destinatario().endereco().numero());

          if (request.destinatario().endereco().complemento() != null) {
            payload.put(
                "complemento_destinatario",
                request.destinatario().endereco().complemento());
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

      // =========================
      // FRETE
      // =========================
      payload.put("modalidade_frete", "9");

      // =========================
      // ITENS
      // =========================
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

          itemMap.put("icms_situacao_tributaria", item.icmsSituacaoTributaria());
          itemMap.put("icms_origem", item.icmsOrigem());
          itemMap.put("pis_situacao_tributaria", item.pisSituacaoTributaria());
          itemMap.put("cofins_situacao_tributaria", item.cofinsSituacaoTributaria());

          // =========================
          // IBS / CBS — Reforma Tributária
          // Obrigatório a partir de 01/04/2026 (NT 2025.002)
          // Calculado aqui com 4 casas (HALF_UP).
          // Os totais do nível raiz NÃO são enviados — o FocusNFe
          // os calcula automaticamente, evitando divergência na SEFAZ.
          // =========================
          BigDecimal base = item.valorBruto() != null
              ? item.valorBruto()
              : item.quantidadeComercial().multiply(item.valorUnitarioComercial());

          BigDecimal cbsValor = base
              .multiply(CBS_ALIQUOTA)
              .divide(CEM, 2, RoundingMode.HALF_UP);

          BigDecimal ibsUfValor = base
              .multiply(IBS_UF_ALIQUOTA)
              .divide(CEM, 2, RoundingMode.HALF_UP);

          BigDecimal ibsMunValor = base
              .multiply(IBS_MUN_ALIQUOTA)
              .divide(CEM, 4, RoundingMode.HALF_UP);

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

          items.add(itemMap);
        }

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

      // =========================
      // 🔥 FAIL-SAFE CRÍTICO (IBS/CBS)
      // Garante que nenhum total de nível raiz seja enviado,
      // seja por serialização acidental de DTOs, putAll ou
      // qualquer outro caminho não previsto.
      // =========================
      payload.remove("ibs_cbs_base_calculo");
      payload.remove("cbs_valor_total");
      payload.remove("ibs_uf_valor_total");
      payload.remove("ibs_valor_total");
      payload.remove("ibs_cbs_is_valor_total");

      return objectMapper.writeValueAsString(payload);

    } catch (Exception e) {
      throw new InvoiceException("Erro ao construir payload da NFe", e);
    }
  }
}
