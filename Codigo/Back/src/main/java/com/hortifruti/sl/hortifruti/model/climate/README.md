# com.hortifruti.sl.hortifruti.model.climate

Entidade e enums usados na recomendação de produtos com base no clima (integração OpenWeather), classificando produtos por categoria de temperatura e sazonalidade.

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `ClimateProduct.java` | `@Entity` (`products`) | Produto cadastrado para recomendação climática. Guarda `name`, `temperatureCategory` (`@Enumerated` de `TemperatureCategory`) e listas `peakSalesMonths`/`lowSalesMonths` (`List<Month>`) persistidas via `MonthListConverter`. |
| `TemperatureCategory.java` | Enum | Categoriza a temperatura média em `CONGELANDO` (0-5°C), `FRIO` (6-14°C), `AMENO` (15-24°C), `QUENTE` (25-50°C). Método `fromTemperature(double)` determina a categoria a partir de uma temperatura. |
| `Month.java` | Enum | Meses do ano (`JANEIRO`...`DEZEMBRO`) com número e nome de exibição. Métodos utilitários `getCurrentMonth()` e `isInSeason(Set<Month>)` para checar sazonalidade. |
| `MonthListConverter.java` | `@Converter` (`AttributeConverter<List<Month>, String>`) | Converte `List<Month>` para/de string separada por vírgula, usado por `ClimateProduct.peakSalesMonths`/`lowSalesMonths`. |
| `RecommendationTag.java` | Enum | Classifica a qualidade de uma recomendação de produto: `BOM`, `MEDIO`, `RUIM`, cada um com `displayName` e `description` documentados via `@Schema` (Swagger). Método `fromScore(double)` mapeia pontuação (0-25) para a tag correspondente. |
