package com.hortifruti.sl.hortifruti.dto.purchase.tabelapreco;

import com.hortifruti.sl.hortifruti.model.purchase.StatusMatchItemTabelaPreco;
import java.math.BigDecimal;

public record TabelaPrecoClienteItemResponse(
    Long id,
    String codigoProdutoCliente,
    String nomeProdutoCliente,
    BigDecimal preco,
    Long fiscalProductId,
    String fiscalProductCodigo,
    String fiscalProductDescricao,
    Double confiancaMatching,
    StatusMatchItemTabelaPreco statusMatch) {}
