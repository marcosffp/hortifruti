# com.hortifruti.sl.hortifruti.config.bb

Integração com a API do Banco do Brasil (Extratos v2), usada na conciliação bancária. Exige mTLS com o certificado e-CNPJ da empresa, reaproveitado do mesmo arquivo PFX usado para o Sicoob.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `BBEndpoints.java` | classe utilitária (final, construtor privado) | Resolve as URLs de OAuth2 e de Extratos do BB a partir do ambiente configurado (`hml`/`prod`), lançando `BBApiException` para valor inválido. |
| `BBEnvironmentGuard.java` | `@Component` | Espelha `sicoob.SicoobEnvironmentGuard`: fora do profile "prod" (fail-closed), bloqueia saldo/extrato/import reais do BB, retornando resultado vazio sem chamar a API nem gravar nada. |
| `BBExtratoClient.java` | `@Component` | Cliente da API Extratos v2: busca uma página (`getExtratoPage`) ou o período completo paginando automaticamente (`getExtratoPeriodo`, até 40 páginas). Agência e conta são fixas por configuração de servidor (nunca vêm do chamador), por segurança. Faz retry único após invalidar o token em caso de 401. |
| `BBSSLConfig.java` | `@Configuration` | Declara o bean `bbRestTemplate` via `MtlsRestTemplateFactory`, com pool de conexões próprio (10 total / 5 por rota) e exceções específicas (`BBApiException`). |
| `BBToken.java` | `@Component` | Obtém e cacheia em memória o `access_token` OAuth2 (`client_credentials`) da API do BB, renovando automaticamente antes de expirar (margem de 30s) ou sob invalidação explícita após 401. |
