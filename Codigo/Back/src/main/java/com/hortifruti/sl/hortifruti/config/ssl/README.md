# com.hortifruti.sl.hortifruti.config.ssl

Infraestrutura compartilhada de mTLS (mutual TLS), reaproveitada pelas integrações do Banco do Brasil (`config.bb`) e do Sicoob/boleto (`config.billet`), que usam o mesmo certificado e-CNPJ da empresa.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `MtlsConnectionSettings.java` | `@Builder` (classe de parâmetros) | Encapsula os parâmetros que variam entre os clientes mTLS: tamanho do pool de conexões (`maxConnTotal`/`maxConnPerRoute`), keep-alive, headers padrão, e as factories de exceção (certificado ausente / erro de configuração) específicas de cada integração. |
| `MtlsRestTemplateFactory.java` | `@Component` | Monta um `RestTemplate` com `SSLContext` carregado a partir do PFX (decodificado via `Base64FileDecoder`) e senha (`password.pfx`), TLS 1.2/1.3, pool de conexões via Apache HttpClient 5, a partir de um `MtlsConnectionSettings`. Usado por `BBSSLConfig` e `BilletSSLConfig`. |
