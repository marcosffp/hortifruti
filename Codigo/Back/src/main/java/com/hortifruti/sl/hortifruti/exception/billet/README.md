# com.hortifruti.sl.hortifruti.exception.billet

Exceção relacionada à emissão e gestão de boletos via Sicoob.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `BilletException.java` | Exceção (`extends RuntimeException`) | Lançada em falhas na integração com a API do Sicoob para geração, consulta ou baixa de boletos (ex.: boleto já baixado/pago, erro de comunicação). Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**, com log de erro. |
