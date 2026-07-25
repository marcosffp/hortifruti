# com.hortifruti.sl.hortifruti.config.sicoob

Cliente da API Conta Corrente v4 do Sicoob (extrato bancário por mês/ano), usada na conciliação bancária como alternativa/complemento ao extrato do BB.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `SicoobExtratoClient.java` | `@Component` | Consulta o extrato mensal da conta corrente Sicoob (`getExtrato`), com retry único após invalidar o token em 401. Reaproveita o token e o `RestTemplate` mTLS já usados para boleto (`SicoobToken`, bean `billetRestTemplate`), mas exige o header `client_id` na própria requisição (diferente das chamadas de boleto). |
