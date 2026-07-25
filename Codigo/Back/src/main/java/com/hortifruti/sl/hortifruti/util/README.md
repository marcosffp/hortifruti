# com.hortifruti.sl.hortifruti.util

Utilitários estáticos sem estado, compartilhados entre múltiplos serviços (arquivos, requisições HTTP, PDF, formatação de extrato Sicoob e classificação de transações bancárias).

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `FileZipUtils.java` | classe utilitária (construtor privado) | Salva bytes em arquivo criando diretórios pai (`saveFile`), compacta uma pasta inteira em `.zip` (`compressFolder`/`zipFolder`) e capitaliza a primeira letra de uma string. Usado pelos serviços de exportação. |
| `HttpRequestUtils.java` | classe utilitária (final, construtor privado) | Resolve o IP real do cliente a partir do header `X-Forwarded-For` (a aplicação roda atrás de proxies — Railway e rewrite do Next.js — então `getRemoteAddr()` sempre retornaria o IP do proxy) e o `User-Agent` da requisição. Usado por `Auth`/`LoginProtectionService` para rate limiting e lockout por IP. |
| `PdfUtil.java` | `@Component` (métodos estáticos) | Extrai texto de um PDF enviado via `MultipartFile` (Apache PDFBox) e busca o valor associado a uma palavra-chave em uma linha no formato `chave: valor`, lançando `PurchaseException` se não encontrar. Usado na leitura de notas/comprovantes de compra. |
| `SicoobExtratoFormatUtil.java` | classe utilitária (final, construtor privado) | Formata valores monetários e datas no padrão do extrato do Sicoob (ex.: `"R$ 1.234,56 C"`), compartilhado entre geração de PDF e Excel do extrato. |
| `TransactionUtil.java` | `@Component` (métodos estáticos) | Lógica de classificação de transações bancárias: mapa de palavras-chave → `Category` (ex.: "cielo" → Vendas Cartão, "cemig" → Cemig), geração de hash SHA-256 para deduplicação de transações (`generateTransactionHash`), filtro de transações já existentes no banco, parsing de valor/data/tipo (débito/crédito) do formato do extrato. |
