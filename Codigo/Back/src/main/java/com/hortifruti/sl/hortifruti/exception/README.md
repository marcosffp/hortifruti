# com.hortifruti.sl.hortifruti.exception

Pacote raiz de exceções da aplicação. Contém o handler global (`@RestControllerAdvice`) que traduz as exceções de negócio de cada subpacote em respostas HTTP padronizadas (JSON com `error`/`message`), além de erros genéricos do Spring (validação, integridade de dados, acesso negado, etc).

## Arquivos

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `GlobalExceptionHandler.java` | `@RestControllerAdvice` | Centraliza o tratamento de exceções da API. Mapeia cada exceção de negócio (dos subpacotes `auth`, `backup`, `bb`, `billet`, `climate`, `finance`, `freight`, `invoice`, `notification`, `product`, `purchase`, `sicoob`, `storage`, `user`) para um status HTTP e corpo JSON padronizado (`error`, `message`). Trata também `AccountLockedException` (401 + `retryAfter`), `AccessDeniedException` (403, pois `@PreAuthorize` nega via AOP e não chega ao filtro do Spring Security), `MethodArgumentNotValidException` (400), `DataIntegrityViolationException` (409 para duplicidade, 400 nos demais casos), `HttpMediaTypeNotSupportedException` (415), `DataAccessResourceFailureException` (503) e um catch-all `Exception` (500). Fixa `Content-Type: application/json` na resposta de erro para não colidir com endpoints que declaram `produces = "application/zip"`. |

## Subpacotes

- `auth/` — exceções de autenticação, token e bloqueio de conta por tentativas de login.
- `backup/` — exceção de falha no backup para o Google Drive.
- `bb/` — exceção de falha na integração com a API do Banco do Brasil.
- `billet/` — exceção de falha na integração com boletos Sicoob.
- `climate/` — exceções de produto, recomendação climática e API do OpenWeather.
- `finance/` — exceção de erro no processamento de transações financeiras.
- `freight/` — exceções de cálculo de frete e de distância (Google Maps).
- `invoice/` — exceções de nota fiscal (Focus NFe), incluindo cancelamento já processado.
- `notification/` — exceção de falha no envio de notificações.
- `product/` — exceção de arquivo inválido no import de conversão caixa→kg de produtos fiscais.
- `purchase/` — exceções de compra, cliente e agrupamento de pontuação combinada.
- `sicoob/` — exceção de falha ao consultar extrato Sicoob.
- `storage/` — exceção de falha no armazenamento (Cloudflare R2).
- `user/` — exceção de erro relacionado a usuários.
