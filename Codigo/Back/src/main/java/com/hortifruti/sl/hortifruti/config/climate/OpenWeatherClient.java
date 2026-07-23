package com.hortifruti.sl.hortifruti.config.climate;

import com.hortifruti.sl.hortifruti.exception.climate.WeatherApiException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class OpenWeatherClient {
  private static final Logger logger = LoggerFactory.getLogger(OpenWeatherClient.class);

  @Value("${app.openweather.apiKey}")
  private String apiKey;

  @Value("${app.openweather.city}")
  private String city;

  @Value("${app.openweather.units}")
  private String units;

  @Value("${app.openweather.lang}")
  private String lang;

  @Value("${app.openweather.forecastUrl}")
  private String forecastUrl;

  @Qualifier("genericRestTemplate")
  private final RestTemplate restTemplate;

  /**
   * Busca a previsão do tempo para 5 dias, em intervalos de 3 horas
   *
   * @return Mapa contendo os dados da previsão
   * @throws WeatherApiException se ocorrer um erro na chamada à API
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> fetch5DayForecast() throws WeatherApiException {
    String encodedCity = city.replace(" ", "+");

    String url =
        forecastUrl
            + "?q="
            + encodedCity
            + "&appid="
            + apiKey
            + "&units="
            + units
            + "&lang="
            + lang;

    try {
      Map<String, Object> response = restTemplate.getForObject(url, Map.class);

      if (response != null) {
        return response;
      } else {
        throw new WeatherApiException("Resposta nula da API OpenWeather");
      }
    } catch (RestClientException e) {
      String sanitizedMessage = redactApiKey(e.getMessage());
      logger.error("Erro ao chamar a API OpenWeather: {}", sanitizedMessage);
      String errorDetails = "";
      if (sanitizedMessage != null && sanitizedMessage.contains("404 Not Found")) {
        errorDetails = sanitizedMessage.replaceAll(".*404 Not Found: \"(.*)\".*", "$1");
        logger.error("Detalhes do erro 404: {}", errorDetails);
      }
      throw new WeatherApiException("Falha ao buscar dados do clima: " + sanitizedMessage, e);
    }
  }

  /**
   * Remove a API key de uma mensagem antes de logar — a URL da OpenWeather carrega a key na query
   * string, e mensagens de {@link RestClientException} costumam ecoar a URL completa.
   */
  private String redactApiKey(String message) {
    if (message == null) {
      return null;
    }
    String redacted = message.replace(apiKey, "***");
    return redacted.replaceAll("(?i)(appid=)[^&\"\\s]*", "$1***");
  }
}
