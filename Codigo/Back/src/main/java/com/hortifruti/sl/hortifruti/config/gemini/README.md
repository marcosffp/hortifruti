# com.hortifruti.sl.hortifruti.config.gemini

Configuração do cliente HTTP usado pela extração de notas via Gemini Vision (`GeminiExtractionService`, em `service.purchase`).

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `GeminiRestTemplateConfig.java` | `@Configuration` | Declara o bean `geminiRestTemplate`, com timeout de conexão/leitura vindo de `gemini.timeout.ms` (não usa o `genericRestTemplate` compartilhado porque o envio de imagem pode legitimamente demorar mais que as demais integrações). |
