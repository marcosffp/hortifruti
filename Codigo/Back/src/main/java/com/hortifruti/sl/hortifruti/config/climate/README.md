# com.hortifruti.sl.hortifruti.config.climate

Integração com a API OpenWeather, usada para prever demanda de produtos hortifrutigranjeiros conforme a previsão do tempo local.

| Arquivo | Tipo | Responsabilidade |
| --- | --- | --- |
| `OpenWeatherClient.java` | `@Component` | Busca a previsão do tempo de 5 dias (intervalos de 3h) para a cidade configurada (Santa Luzia, MG). Redige a API key das mensagens de log/erro antes de logar, já que a URL da OpenWeather carrega a chave na query string. |
