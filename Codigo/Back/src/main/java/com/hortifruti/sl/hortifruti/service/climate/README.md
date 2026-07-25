# com.hortifruti.sl.hortifruti.service.climate

Recomenda produtos hortifrúti com base na previsão do tempo (integração OpenWeather, 5 dias) e na sazonalidade cadastrada de cada produto, além de manter o CRUD dos produtos climáticos.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `WeatherForecastService.java` | `@Service` | Consulta `OpenWeatherClient` e agrega a previsão bruta (de 3 em 3h) em até 5 dias, calculando min/max/média de temperatura, sensação térmica, umidade, chuva acumulada, vento e a descrição/ícone predominante do dia. |
| `ClimateProductRecommendationService.java` | `@Service` (`readOnly`) | Calcula um score de recomendação por produto combinando proximidade da categoria de temperatura do produto com o clima atual (peso 0.7, com decaimento por distância de categoria) e sazonalidade do mês atual — pico/baixa/média (peso 0.3); classifica o resultado em uma `RecommendationTag` e ordena por score. Também permite recomendação por categoria de temperatura específica ou por data (usa a previsão de 5 dias). |
| `ProductService.java` | `@Service` (`readOnly`) | CRUD de `ClimateProduct`: listagem paginada/completa, busca por ID/nome, criação e atualização com validação de nome duplicado (case-insensitive), exclusão e contagem. |
