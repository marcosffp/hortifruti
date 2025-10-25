package com.hortifruti.sl.hortifruti.service.billet;

import org.springframework.stereotype.Component;

import lombok.Getter;

import org.springframework.beans.factory.annotation.Value;

@Component
@Getter
public class BilletConstants {
    @Value("${sicoob.num.cliente}")
    private Integer clientNumber;

    @Value("${sicoob.num.conta.corrente}")
    private Integer accountNumber;

    private Integer MODALITY_CODE = 1;
    private String BASE_URL = "/cobranca-bancaria/v3/";
}
