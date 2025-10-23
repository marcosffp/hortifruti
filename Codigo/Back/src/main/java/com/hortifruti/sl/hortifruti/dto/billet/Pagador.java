package com.hortifruti.sl.hortifruti.dto.billet;

public record Pagador(
    String numeroCpfCnpj,
    String nome,
    String endereco,
    String bairro,
    String cidade,
    String cep,
    String uf) {}
