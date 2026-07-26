# com.hortifruti.sl.hortifruti.service.finance.sicoob

Integração com a API de conta corrente do Sicoob: busca de extrato, geração de PDF/Excel no
layout original do Sicoob e importação/deduplicação de transações.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `SicoobExtratoExcelGenerator.java` | `@Component` | Gera Excel (.xlsx) do extrato no layout original (título, cabeçalho Data/Documento/Histórico/Valor), destacando linhas de saldo e débitos em vermelho. |
| `SicoobExtratoLayoutService.java` | `@Service` | Monta as linhas de exibição (compartilhadas por PDF e Excel), calculando o saldo acumulado por dia a partir de `saldoAnterior` (a API só retorna saldo do período inteiro, não por dia); loga divergência se o saldo calculado não bater com `saldoAtual` da API. |
| `SicoobExtratoParsingUtil.java` | classe utilitária (`final`) | Fonte única de parsing dos campos da API Sicoob (tipo crédito/débito com fallback pelo sinal do valor, valor decimal sem separador de milhar, datas em formato ISO ou BR); reaproveitada tanto na importação quanto na exportação para não divergir formato em dois lugares. |
| `SicoobExtratoPdfGenerator.java` | `@Component` | Gera PDF do extrato no layout visual original do Sicoob (cabeçalho, tabela com sub-linhas, seção RESUMO com saldos/limites); estado de escrita isolado por chamada via classe interna `PageWriter`. |
| `SicoobStatementService.java` | `@Service` | Orquestra o fluxo completo do Sicoob: busca extrato do período (mês/ano + dia inicial/final opcionais), gera e salva PDF no R2, persiste `Statement` e importa transações novas deduplicadas; também expõe exportação de PDF/Excel sem persistência. |
| `TransactionSicoobApiService.java` | `@Service` | Converte transações da API em `Transaction`; gera hash de deduplicação a partir do `transactionId` da própria API (torna reconsultas de períodos sobrepostos idempotentes). |
