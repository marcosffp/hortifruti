# com.hortifruti.sl.hortifruti.dto.billet

DTOs do fluxo de emissão e consulta de boletos bancários via API do Sicoob. Cobrem o payload
enviado ao Sicoob (dados do boleto e do pagador), as respostas de emissão/consulta e a listagem de
boletos em aberto vinculados a compras (`combinedScore`).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `BilletRequest.java` | record | Payload completo enviado à API do Sicoob para emitir um boleto: dados de conta/modalidade, número do documento (`seuNumero`), valor, datas de emissão/vencimento, configuração de desconto/multa/juros, dados do `Pagador` e flag para gerar PDF. |
| `BilletRequestSimplified.java` | record | Versão reduzida de `BilletRequest`, usada internamente com apenas os campos essenciais: emissão, número do documento, valor, vencimento e pagador. |
| `BilletResponse.java` | record | Resposta de emissão/consulta de boleto: nome do pagador, datas, `seuNumero`/`nossoNumero`, situação do boleto, valor e o id da compra (`combinedScoreId`) associada. |
| `OpenBilletResponse.java` | record | Representa um boleto em aberto vinculado a uma compra: id da compra e do cliente, nome do cliente, valor total, vencimento, número do documento e se já foi confirmado no Sicoob. |
| `Pagador.java` | record | Dados do pagador do boleto exigidos pelo Sicoob: CPF/CNPJ, nome e endereço completo (bairro, cidade, CEP, UF). |
