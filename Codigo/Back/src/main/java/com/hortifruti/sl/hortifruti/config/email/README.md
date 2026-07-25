# com.hortifruti.sl.hortifruti.config.email

Configuração do `RestTemplate` genérico usado pelos serviços de notificação por e-mail (SendGrid/Gmail API).

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `RestTemplateConfig.java` | `@Configuration` | Declara o bean `genericRestTemplate` (timeouts de conexão/leitura de 30s). O nome é deliberadamente diferente de `restTemplate` para não colidir por autowiring de nome com os beans mTLS (`bbRestTemplate`, `billetRestTemplate`) caso um `@Qualifier` seja perdido — incidente já ocorrido em produção. |
