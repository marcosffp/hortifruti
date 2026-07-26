# com.hortifruti.sl.hortifruti.config.sicoob

Cliente da API Conta Corrente v4 do Sicoob (extrato bancário por mês/ano), usada na conciliação bancária como alternativa/complemento ao extrato do BB.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `SicoobExtratoClient.java` | `@Component` | Consulta o extrato mensal da conta corrente Sicoob (`getExtrato`), com retry único após invalidar o token em 401. Reaproveita o token e o `RestTemplate` mTLS já usados para boleto (`SicoobToken`, bean `billetRestTemplate`), mas exige o header `client_id` na própria requisição (diferente das chamadas de boleto). |
| `SicoobEnvironmentGuard.java` | `@Component` | O Sicoob não oferece ambiente de homologação/sandbox. Fora do profile "prod" (fail-closed), bloqueia boleto/extrato reais, retornando resultado vazio sem chamar o Sicoob nem gravar nada. |
