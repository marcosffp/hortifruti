package com.hortifruti.sl.hortifruti.config.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hortifruti.sl.hortifruti.exception.WeatherApiException;

/**
 * Cliente para a API do OpenWeather
 * Responsável por buscar dados de previsão do tempo
 */
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
     * @return Mapa contendo os dados da previsão
     * @throws WeatherApiException se ocorrer um erro na chamada à API
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetch5DayForecast() throws WeatherApiException {
        // Codificar a cidade para URL de forma mais explícita
        String encodedCity = city.replace(" ", "+");
        
        // Construir a URL de forma mais simples, evitando possíveis problemas de codificação
        String url = forecastUrl + 
                "?q=" + encodedCity + 
                "&appid=" + apiKey + 
                "&units=" + units + 
                "&lang=" + lang;
        
        // Log completo da URL para diagnóstico (exceto a chave API)
        String urlLog = url.replace(apiKey, "API_KEY_HIDDEN");
        logger.info("Chamando API OpenWeather URL: {}", urlLog);
        
        try {
            // Configuração básica para a requisição HTTP
            RestTemplate restTemplate = new RestTemplate();
            
            // Configurar timeout mais longo
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(30000); // 30 segundos
            factory.setReadTimeout(30000);
            restTemplate.setRequestFactory(factory);
            
            // Fazer requisição direta, método mais simples
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                logger.info("Resposta recebida da API OpenWeather com sucesso: {}", response.get("cod"));
                return response;
            } else {
                logger.error("Resposta nula da API OpenWeather");
                throw new WeatherApiException("Resposta nula da API OpenWeather");
            }
        } catch (RestClientException e) {
            logger.error("Erro ao chamar a API OpenWeather: {}", e.getMessage(), e);
            // Captura o corpo da resposta de erro, se disponível
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
