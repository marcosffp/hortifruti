# com.hortifruti.sl.hortifruti.service.finance.bb

Integração com a API Extratos v2 do Banco do Brasil (mTLS): consulta de saldo do dia, busca de
lançamentos, geração de PDF/Excel do extrato no layout original do BB e importação/deduplicação de
transações.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `BBExtratoExcelGenerator.java` | `@Component` | Gera Excel (.xlsx) do extrato a partir dos lançamentos da API, no layout original (título, cabeçalho Data/Ag.Origem/Lote/Documento/Histórico/Valor/Saldo), destacando linhas de saldo e débitos em vermelho. |
| `BBExtratoLayoutService.java` | `@Service` | Monta as linhas de exibição do extrato (compartilhadas por PDF e Excel), calculando saldo acumulado após cada lançamento (a API não devolve esse saldo por linha) e filtrando só lançamentos contabilizados (`indicadorTipoLancamento == "1"`). |
| `BBExtratoParsingUtil.java` | classe utilitária (`final`) | Parsing dos campos crus da API (data no formato DDMMAAAA, valor decimal com sinal D/C); ressalva de que só o endpoint de saldo foi validado contra resposta real, os demais campos seguem a documentação. |
| `BBExtratoPdfGenerator.java` | `@Component` | Gera PDF do extrato a partir dos lançamentos da API, no layout visual original do BB (cabeçalho, tabela com sub-linha de complemento); estado de escrita isolado por chamada via classe interna `PageWriter`. |
| `BBSaldoService.java` | `@Service` | Consulta o saldo disponível da conta corrente PJ do dia atual (sem aceitar agência/conta/data do chamador, por segurança); cache em memória (TTL configurável); percorre páginas da API filtrando históricos de saldo (0/999) e tipos detalhados (SA/RA/LE/AP/SD/LD/LC/LU). |
| `BBStatementService.java` | `@Service` | Orquestra o fluxo completo do BB: busca lançamentos do período (mesmo mês, não pode ser data futura), gera e salva PDF no R2, persiste `Statement` e importa transações novas deduplicadas; também expõe exportação de PDF/Excel sem persistência. |
| `TransactionBBApiService.java` | `@Service` | Converte lançamentos crus da API em `Transaction`: descarta lançamentos não contabilizados e marcadores de saldo (histórico "0"/"999"); gera hash de deduplicação a partir de `TextoIdentificadorUnicoTransacao` (com fallback textual data+valor+histórico). |
