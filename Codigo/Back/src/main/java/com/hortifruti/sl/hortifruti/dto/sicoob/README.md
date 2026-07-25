# com.hortifruti.sl.hortifruti.dto.sicoob

DTOs que espelham a resposta da API de extrato bancário do Sicoob (`GET
/conta-corrente/v4/extrato/{mes}/{ano}`), usados para desserializar o JSON retornado e para formatar
o extrato para exibição (PDF/Excel).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `SicoobExtratoEnvelope.java` | record | Envelope real da resposta 200 do Sicoob: os dados do extrato vêm dentro de `resultado` (não na raiz do JSON, ao contrário do documentado). `mensagens` não é usado hoje e é mantido como `JsonNode` para não travar em formatos inesperados. |
| `SicoobExtratoLinha.java` | record | Linha do extrato pronta para exibição: data, documento, histórico, valor e flag `destaque`. Sub-linhas de complemento e linhas de "SALDO DO DIA"/"SALDO ANTERIOR" vêm com `data`/`documento`/`valor` nulos. |
| `SicoobExtratoResponse.java` | record | Corpo da resposta do extrato Sicoob: saldo atual, bloqueado, limite, anterior, bloqueio judicial (atual e anterior) e lista de `SicoobExtratoTransacao`. Campos com os mesmos nomes do JSON original para desserialização direta. |
| `SicoobExtratoTransacao.java` | record | Um item de `transacoes[]` do extrato Sicoob: id da transação, tipo, valor, data, data do lote, descrição, número do documento, CPF/CNPJ e informação complementar. |
| `SicoobImportSummary.java` | record | Resumo de uma busca de extrato via API do Sicoob: id do statement, flag `alreadyProcessed`, período, totais buscados/salvos/duplicados ignorados e somas de entradas/saídas. |
