# com.hortifruti.sl.hortifruti.config

Configurações gerais do Spring (beans, clientes HTTP, inicialização de dados) e utilitários de infraestrutura que não pertencem a uma integração específica. Integrações mais específicas (autenticação, BB, Sicoob/boleto, clima, e-mail, frete, SSL/mTLS, storage) ficam em subpacotes dedicados.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `Base64FileDecoder.java` | `@Component` | Decodifica para arquivos temporários em disco os segredos Base64 vindos de variáveis de ambiente: credenciais do Google Drive, certificado PFX (e-CNPJ, usado por Sicoob/BB) e chave PEM. Usado na inicialização (`UserInitializer`) e por `MtlsRestTemplateFactory`. |
| `FocusNfeApiClient.java` | `@Component` | Cliente HTTP (Basic Auth) da API Focus NFe: emissão (`sendRequest`), consulta (`sendGetRequest`) e cancelamento (`cancelInvoice`) de NF-e. Extrai `codigo`/`mensagem` do corpo de erro para diferenciar cancelamento duplicado (`InvoiceAlreadyCancelledException`) de outras falhas. |
| `SwaggerConfig.java` | `@Component` | Define o bean `OpenAPI` customizado (título, versão, esquema de segurança Bearer JWT e suporte a upload multipart) usado pelo springdoc/Swagger UI. |
| `UserInitializer.java` | `@Component` (`CommandLineRunner`, `@Order(1)`) | Roda na subida da aplicação: decodifica os arquivos Base64, cria usuários iniciais (fixos em perfil `local`; conta admin com senha aleatória em produção), popula/repopula produtos de exemplo (`ClimateProduct`) quando ausentes ou corrompidos, e cria a configuração padrão de frete (`FreightConfig`) se não existir. |
| `WebClientConfig.java` | `@Configuration` | Beans de `WebClient`/`WebClient.Builder` reativo, com limite de memória do codec elevado para 10MB. |

## Subpacotes

- `auth/` — autenticação, JWT, proteção contra brute-force, rate limiting e refresh tokens (ver `auth/README.md`).
- `bb/` — cliente e configuração mTLS da API de Extratos do Banco do Brasil (ver `bb/README.md`).
- `billet/` — cliente HTTP, SSL e token OAuth2 da API de boletos do Sicoob (ver `billet/README.md`).
- `climate/` — cliente da API OpenWeather (ver `climate/README.md`).
- `email/` — `RestTemplate` genérico usado pelo envio de e-mail (ver `email/README.md`).
- `freight/` — propriedades de configuração de cálculo de frete (ver `freight/README.md`).
- `gemini/` — `RestTemplate` dedicado à extração de notas via Gemini Vision, com timeout próprio (ver `gemini/README.md`).
- `sicoob/` — cliente da API de Extrato (Conta Corrente v4) do Sicoob (ver `sicoob/README.md`).
- `ssl/` — infraestrutura compartilhada de mTLS reaproveitada por BB e Sicoob (ver `ssl/README.md`).
- `storage/` — configuração do cliente S3 para Cloudflare R2 (ver `storage/README.md`).
