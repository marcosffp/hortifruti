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
 * <h3>Estratégia de cálculo para evitar rejeição SEFAZ 1076</h3>
 *
 * O erro 1076 ocorre quando o total informado no XML diverge da soma dos itens.
 * O FocusNFe NÃO calcula os totais automaticamente — ele usa exatamente
 * o que é enviado no payload.
 *
 * Solução adotada:
 * 1. Calcular CBS e IBS por item com 4 casas decimais (HALF_UP).
 * 2. Somar os valores dos itens para obter os totais do nível raiz.
 * 3. Enviar os totais do nível raiz como a soma exata dos itens.
 *
 * Dessa forma, o FocusNFe escreve no XML exatamente o que foi enviado,
 * e a SEFAZ encontra consistência entre itens e totais.
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
      // ITENS + ACUMULADORES IBS/CBS
      // =========================
      BigDecimal totalBase   = BigDecimal.ZERO;
      BigDecimal totalCbs    = BigDecimal.ZERO;
      BigDecimal totalIbsUf  = BigDecimal.ZERO;
      BigDecimal totalIbsMun = BigDecimal.ZERO;

      if (request.items() != null && !request.items().isEmpty()) {
        List<Map<String, Object>> items = new ArrayList<>();

        for (ItemRequest item : request.items()) {
          Map<String, Object> itemMap = new HashMap<>();

          itemMap.put("numero_item",              items.size() + 1);
          itemMap.put("codigo_produto",            item.codigoProduto());
          itemMap.put("descricao",                 item.descricao());
          itemMap.put("codigo_ncm",                item.ncm());
          itemMap.put("cfop",                      item.cfop());

          itemMap.put("unidade_comercial",         item.unidadeComercial());
          itemMap.put("quantidade_comercial",      item.quantidadeComercial());
          itemMap.put("valor_unitario_comercial",  item.valorUnitarioComercial());
          itemMap.put("valor_bruto",               item.valorBruto());

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

          itemMap.put("icms_situacao_tributaria",  item.icmsSituacaoTributaria());
          itemMap.put("icms_origem",               item.icmsOrigem());
          itemMap.put("pis_situacao_tributaria",   item.pisSituacaoTributaria());
          itemMap.put("cofins_situacao_tributaria", item.cofinsSituacaoTributaria());

          // =========================
          // IBS / CBS por item
          // 4 casas decimais (HALF_UP) para máxima precisão.
          // Os totais do nível raiz serão a soma exata desses valores,
          // garantindo consistência com o XML e aprovação na SEFAZ.
          // =========================
          BigDecimal base = item.valorBruto() != null
              ? item.valorBruto()
              : item.quantidadeComercial().multiply(item.valorUnitarioComercial());

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

          // Acumular para os totais do nível raiz
          totalBase   = totalBase.add(base);
          totalCbs    = totalCbs.add(cbsValor);
          totalIbsUf  = totalIbsUf.add(ibsUfValor);
          totalIbsMun = totalIbsMun.add(ibsMunValor);

          items.add(itemMap);
        }

        payload.put("items", items);
      }

      // =========================
      // TOTAIS IBS/CBS — NÍVEL RAIZ
      //
      // São a soma exata dos valores calculados por item.
      // O FocusNFe usa esses valores diretamente no XML —
      // não os recalcula — portanto precisam ser enviados.
      // Manter consistência com os itens elimina a rejeição 1076.
      // =========================
      BigDecimal totalIbs    = totalIbsUf.add(totalIbsMun);
      BigDecimal totalIbsCbs = totalCbs.add(totalIbs);

      payload.put("ibs_cbs_base_calculo",   totalBase.setScale(2, RoundingMode.HALF_UP));
      payload.put("cbs_valor_total",        totalCbs.setScale(4, RoundingMode.HALF_UP));
      payload.put("ibs_uf_valor_total",     totalIbsUf.setScale(4, RoundingMode.HALF_UP));
      payload.put("ibs_valor_total",        totalIbs.setScale(4, RoundingMode.HALF_UP));
      payload.put("ibs_cbs_is_valor_total", totalIbsCbs.setScale(4, RoundingMode.HALF_UP));

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
