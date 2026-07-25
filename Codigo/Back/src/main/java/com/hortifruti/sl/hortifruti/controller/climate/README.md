# com.hortifruti.sl.hortifruti.controller.climate

Endpoints de previsão do tempo (OpenWeather), cadastro de produtos e recomendação de produtos com base em clima/temperatura, todos restritos a usuários MANAGER (exceto a previsão do tempo).

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `ClimateProductRecommendationController.java` | `@RestController` (`/api/recommendations`), `@PreAuthorize hasRole('MANAGER')` | `GET /api/recommendations/by-temperature/{category}` retorna produtos recomendados para uma `TemperatureCategory`; `GET /api/recommendations/by-date?date=YYYY-MM-DD` retorna recomendações buscando o clima da data informada. |
| `ProductController.java` | `@RestController` (`/products`), `@PreAuthorize hasRole('MANAGER')` | CRUD completo de produtos: `GET /products` lista todos; `GET /products/paginated` lista paginado/ordenável; `GET /products/{id}` busca por ID; `GET /products/search?name=` busca por nome; `POST /products` cria; `PUT /products/{id}` atualiza; `DELETE /products/{id}` remove; `GET /products/count` retorna contagem total. |
| `WeatherForecastController.java` | `@RestController` (`/api/weather`) | `GET /api/weather/forecast/5days` retorna a previsão de 5 dias via `WeatherForecastService` (OpenWeather), respondendo 500 em caso de falha do serviço externo. |
