package com.hortifruti.sl.hortifruti.service.invoice.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortifruti.sl.hortifruti.dto.invoice.IssueInvoiceRequest;
import com.hortifruti.sl.hortifruti.dto.invoice.ItemRequest;
import com.hortifruti.sl.hortifruti.exception.InvoiceException;

@Component
public class InvoicePayload {
        private final ObjectMapper objectMapper = new ObjectMapper();


    @Value("${focus.nfe.token}")
    private String focusNfeToken;

    @Value("${focus.nfe.api.url}")
    private String focusNfeApiUrl;

    @Value("${focus.nfe.environment:homologacao}")
    private String focusNfeEnvironment;

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
                payload.put("indicador_inscricao_estadual_destinatario", request.destinatario().indicadorInscricaoEstadual());
                
                if (request.destinatario().inscricaoEstadual() != null) {
                    payload.put("inscricao_estadual_destinatario", request.destinatario().inscricaoEstadual());
                }
                
            
                if (request.destinatario().endereco() != null) {
                    payload.put("logradouro_destinatario", request.destinatario().endereco().logradouro());
                    payload.put("numero_destinatario", request.destinatario().endereco().numero());
                    if (request.destinatario().endereco().complemento() != null) {
                        payload.put("complemento_destinatario", request.destinatario().endereco().complemento());
                    }
                    payload.put("bairro_destinatario", request.destinatario().endereco().bairro());
                    payload.put("municipio_destinatario", request.destinatario().endereco().municipio());
                    payload.put("uf_destinatario", request.destinatario().endereco().uf());
                    payload.put("cep_destinatario", request.destinatario().endereco().cep());
                    
                    if (request.destinatario().endereco().codigoMunicipio() != null) {
                        payload.put("codigo_municipio_destinatario", request.destinatario().endereco().codigoMunicipio());
                    }
                    
                    payload.put("pais_destinatario", 
                        request.destinatario().endereco().nomePais() != null 
                            ? request.destinatario().endereco().nomePais() 
                            : "Brasil");
                    payload.put("codigo_pais_destinatario", 
                        request.destinatario().endereco().codigoPais() != null 
                            ? request.destinatario().endereco().codigoPais() 
                            : "1058");
                }
            }
            
         
            payload.put("modalidade_frete", "9");
            
            
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
                    
                    itemMap.put("unidade_tributavel", 
                        item.unidadeTributavel() != null 
                            ? item.unidadeTributavel() 
                            : item.unidadeComercial());
                    itemMap.put("quantidade_tributavel", 
                        item.quantidadeTributavel() != null 
                            ? item.quantidadeTributavel() 
                            : item.quantidadeComercial());
                    itemMap.put("valor_unitario_tributavel", 
                        item.valorUnitarioTributavel() != null 
                            ? item.valorUnitarioTributavel() 
                            : item.valorUnitarioComercial());
                    
                    itemMap.put("icms_situacao_tributaria", item.icmsSituacaoTributaria());
                    itemMap.put("icms_origem", item.icmsOrigem());
                    itemMap.put("pis_situacao_tributaria", item.pisSituacaoTributaria());
                    itemMap.put("cofins_situacao_tributaria", item.cofinsSituacaoTributaria());
                    
                    items.add(itemMap);
                }
                payload.put("items", items);
            }
            
            if (request.informacoesAdicionaisContribuinte() != null) {
                payload.put("informacoes_adicionais_contribuinte", request.informacoesAdicionaisContribuinte());
            }
            
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new InvoiceException("Erro ao construir payload", e);
        }
    }
}
