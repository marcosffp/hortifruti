# com.hortifruti.sl.hortifruti.exception.purchase

Exceções do módulo de compras/vendas, clientes e agrupamento de pontuação para cobrança combinada.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `PurchaseException.java` | Exceção (`extends RuntimeException`) | Lançada em erros de processamento de compras (ex.: dados inválidos, status inválido — ver `Status.fromString` em `model.purchase`). Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
| `ClientException.java` | Exceção (`extends RuntimeException`) | Lançada em erros de cadastro/consulta de clientes. Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
| `CombinedScoreException.java` | Exceção (`extends RuntimeException`) | Lançada em falhas no agrupamento de pontuação combinada de compras (`CombinedScore`) para geração conjunta de boleto/nota fiscal. Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
| `InvalidNotaFileException.java` | Exceção (`extends RuntimeException`) | Lançada pelo `NotaUploadService` quando o arquivo de foto de nota está ausente ou não é um JPEG/PNG válido (checagem por magic bytes, não por extensão/Content-Type declarado). Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
| `NotaFileTooLargeException.java` | Exceção (`extends RuntimeException`) | Lançada pelo `NotaUploadService` quando a foto de nota excede `nota.upload.max-size-bytes`. Tratada pelo `GlobalExceptionHandler` com status **413 Content Too Large**. |
| `GeminiExtractionException.java` | Exceção (`extends RuntimeException`) | Lançada pelo `GeminiExtractionService` em falha/timeout da chamada à API do Gemini ou resposta que não pôde ser interpretada. Tratada pelo `GlobalExceptionHandler` com status **502 Bad Gateway**. |
