# com.hortifruti.sl.hortifruti.dto.climate

DTOs do módulo de clima e sazonalidade: previsão do tempo (integração OpenWeather), dados diários de
clima e recomendação de produtos hortifrutigranjeiros de acordo com temperatura/época do ano. A
maioria expõe anotações `@Schema` do Swagger/OpenAPI para documentação automática da API.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `ClimateProductRecommendationDTO.java` | record | Recomendação de produto baseada em clima e sazonalidade: id/nome do produto, categoria de temperatura ideal, pontuação (0-25) e tag de qualidade (`RecommendationTag`). Documentado com `@Schema`. |
| `DayClimateDataDTO.java` | record | Dados climáticos de um dia específico (data, temperaturas mín/máx/média, umidade, chuva, vento, descrição e ícone do clima), usado para trocar dados entre front e back quando um card de clima é clicado. Possui factory `fromDailyForecast` a partir de `WeatherForecastDTO.DailyForecastDTO` e um construtor auxiliar simplificado. |
| `ProductRequest.java` | record | Payload de request para cadastro/edição de produto com sazonalidade: nome (`@NotBlank`, máx. 100 caracteres), categoria de temperatura (`@NotNull`) e listas de meses de pico/baixa venda (normalizadas para lista vazia se nulas no construtor compacto). |
| `ProductResponse.java` | record | Resposta com dados do produto e campos derivados para exibição: nome de exibição da categoria de temperatura e strings formatadas dos meses de pico/baixa venda, calculados em construtor auxiliar. |
| `WeatherForecastDTO.java` | record | Previsão do tempo de 5 dias: cidade, país e lista de `DailyForecastDTO` (record aninhado) com temperaturas, sensação térmica, umidade, chuva, vento e descrição/ícone do clima por dia. |
