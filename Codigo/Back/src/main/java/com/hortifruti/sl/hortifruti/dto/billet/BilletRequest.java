package com.hortifruti.sl.hortifruti.dto.billet;

import java.math.BigDecimal;

public record BilletRequest(
    Integer numeroCliente,
    Integer codigoModalidade,
    Integer numeroContaCorrente,
    String codigoEspecieDocumento,
    String dataEmissao,
    String seuNumero,
    Integer identificacaoEmissaoBoleto,
    Integer identificacaoDistribuicaoBoleto,
    BigDecimal valor,
    String dataVencimento,
    Integer tipoDesconto,
    Integer tipoMulta,
    Integer tipoJurosMora,
    Integer numeroParcela,
    Pagador pagador,
    Boolean gerarPdf) {}
