package com.hortifruti.sl.hortifruti.config.climate;

import com.hortifruti.sl.hortifruti.exception.WeatherApiException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
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

  /**
   * Busca a previsão do tempo para 5 dias, em intervalos de 3 horas
   *
   * @return Mapa contendo os dados da previsão
   * @throws WeatherApiException se ocorrer um erro na chamada à API
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> fetch5DayForecast() throws WeatherApiException {
    // Codificar a cidade para URL de forma mais explícita
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
      RestTemplate restTemplate = new RestTemplate();

      SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
      factory.setConnectTimeout(30000); 
      factory.setReadTimeout(30000);
      restTemplate.setRequestFactory(factory);

      Map<String, Object> response = restTemplate.getForObject(url, Map.class);

      if (response != null) {
        return response;
      } else {
        throw new WeatherApiException("Resposta nula da API OpenWeather");
      }
    } catch (RestClientException e) {
      logger.error("Erro ao chamar a API OpenWeather: {}", e.getMessage(), e);
      String errorDetails = "";
      if (e.getMessage() != null) {
        logger.error("Erro detalhado: {}", e.getMessage());
        if (e.getMessage().contains("404 Not Found")) {
          errorDetails = e.getMessage().replaceAll(".*404 Not Found: \"(.*)\".*", "$1");
          logger.error("Detalhes do erro 404: {}", errorDetails);
        }
      }
      throw new WeatherApiException("Falha ao buscar dados do clima: " + e.getMessage(), e);
    }
  }
}
