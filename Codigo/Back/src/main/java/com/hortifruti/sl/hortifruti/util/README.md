# com.hortifruti.sl.hortifruti.util

Utilitários estáticos sem estado, genuinamente compartilhados entre domínios distintos (arquivos,
PDF, parsing/dedupe de transações). Utilitários usados por um único módulo vivem no pacote desse
módulo — `SicoobExtratoFormatUtil` está em `service.finance` e `HttpRequestUtils` em
`config.auth`.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `FileZipUtils.java` | classe utilitária (construtor privado) | Salva bytes em arquivo criando diretórios pai (`saveFile`), compacta uma pasta inteira em `.zip` (`compressFolder`/`zipFolder`) e capitaliza a primeira letra de uma string. Usado pelos serviços de exportação (finance e invoice/tax). |
| `PdfUtil.java` | `@Component` (métodos estáticos) | Extrai texto de um PDF enviado via `MultipartFile` (Apache PDFBox) e busca o valor associado a uma palavra-chave em uma linha no formato `chave: valor`, lançando `PurchaseException` se não encontrar. Usado na leitura de notas/comprovantes de compra (purchase e finance/sicoob). |
| `TransactionUtil.java` | classe utilitária (final, construtor privado) | Parsing/dedupe genérico de transações bancárias: geração de hash SHA-256 (`generateTransactionHash`), filtro de transações já existentes no banco, parsing de valor/data/tipo (débito/crédito) do formato do extrato. A classificação por categoria (que depende de configuração e nomes de funcionários) foi extraída para `service.finance.transaction.TransactionCategoryClassifier` — não é utilitário genérico, é regra de negócio. |
