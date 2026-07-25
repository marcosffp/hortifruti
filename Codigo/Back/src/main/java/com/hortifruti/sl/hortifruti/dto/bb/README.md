# com.hortifruti.sl.hortifruti.dto.bb

DTOs relacionados à importação de extrato bancário via API do Banco do Brasil (BB). Servem para
apresentar o extrato formatado (PDF/Excel) e resumir o resultado da sincronização com a API.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `BBExtratoLinha.java` | record | Uma linha do extrato do BB pronta para exibição, no mesmo formato do extrato original: data, agência/lote de origem, documento, histórico, valor, saldo acumulado após cada lançamento e flag `destaque`. Diferente do Sicoob, o BB mostra saldo acumulado por lançamento, não só por dia. |
| `BBImportSummary.java` | record | Resumo de uma busca de extrato via API do BB: id do statement, flag `alreadyProcessed` (período já buscado antes, sem nova chamada à API), período (`periodStart`/`periodEnd`), totais buscados/salvos/duplicados ignorados e somas de entradas/saídas. |
