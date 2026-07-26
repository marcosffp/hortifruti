# com.hortifruti.sl.hortifruti.exception.climate

Exceções do módulo de clima e recomendação de produtos sazonais (integração com OpenWeather).

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `ProductException.java` | Exceção (`extends RuntimeException`) | Lançada em erros de cadastro/consulta de produtos climáticos (`ClimateProduct`). Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
| `RecommendationException.java` | Exceção (`extends RuntimeException`) | Lançada em falhas ao gerar recomendações de produtos com base no clima. Tratada pelo `GlobalExceptionHandler` com status **400 Bad Request**. |
| `WeatherApiException.java` | Exceção checada (`extends Exception`) | Lançada em falhas na comunicação com a API do OpenWeather. Diferente das demais exceções do pacote, é checada (não `RuntimeException`) e não possui handler dedicado no `GlobalExceptionHandler`. |
