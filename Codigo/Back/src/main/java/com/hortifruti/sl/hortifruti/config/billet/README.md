# com.hortifruti.sl.hortifruti.config.billet

Infraestrutura de comunicação com a API de boletos (cobrança) do Sicoob: cliente HTTP genérico com retry, configuração mTLS e token OAuth2, além da configuração global do Jackson usada em toda a aplicação.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `BilletHttpClient.java` | `@Component` | Cliente HTTP genérico (GET/POST/PUT/DELETE) para a API do Sicoob, com retry automático (invalida token e tenta uma vez mais em 401) e extração da mensagem de erro real do corpo de resposta (`mensagens[0].mensagem`), evitando mensagens genéricas ao operador. `postCancel` trata especificamente resposta 204. |
| `BilletSSLConfig.java` | `@Configuration` | Declara o bean `billetRestTemplate` via `MtlsRestTemplateFactory`, com pool próprio (20 total/10 por rota), keep-alive de 2 minutos e headers padrão (`Accept`/`Content-Type: application/json`, `User-Agent`). |
| `JacksonConfig.java` | `@Configuration` | Define o `ObjectMapper` `@Primary` global da aplicação: módulo `JavaTimeModule`, datas não seriais como timestamp, tolerância a beans vazios e propriedades desconhecidas, e limite de profundidade de aninhamento elevado (2000). |
| `SicoobToken.java` | `@Component` | Obtém e cacheia o `access_token` OAuth2 (`client_credentials`) da API do Sicoob, com fallback de expiração de 55 minutos quando a resposta não informa `expires_in`. |
