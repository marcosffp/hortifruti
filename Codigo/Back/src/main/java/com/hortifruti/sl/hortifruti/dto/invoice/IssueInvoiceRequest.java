package com.hortifruti.sl.hortifruti.dto.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.util.List;

public record IssueInvoiceRequest(
    @NotNull(message = "ID da compra é obrigatório")
    @JsonProperty("combinedScore_id")
    Long combinedScoreId,

    @NotBlank(message = "Natureza da operação é obrigatória")
    @JsonProperty("natureza_operacao")
    String naturezaOperacao,

    @JsonProperty("data_emissao")
    String dataEmissao, 

    @NotNull(message = "Dados do destinatário são obrigatórios")
    @JsonProperty("destinatario")
    RecipientRequest destinatario,

    @NotEmpty(message = "Lista de itens não pode estar vazia")
    @JsonProperty("items")
    List<ItemRequest> items,

    @JsonProperty("informacoes_adicionais_contribuinte")
    String informacoesAdicionaisContribuinte
) {

}