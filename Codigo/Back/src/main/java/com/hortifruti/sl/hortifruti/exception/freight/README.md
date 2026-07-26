# com.hortifruti.sl.hortifruti.exception.freight

Exceções do módulo de cálculo de frete (integração com Google Maps).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `FreightException.java` | Exceção (`extends RuntimeException`) | Lançada em erros gerais de cálculo/configuração de frete (ex.: `FreightConfig` inválida). Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
| `DistanceException.java` | Exceção (`extends RuntimeException`) | Lançada em falhas ao calcular distância via API do Google Maps. Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
