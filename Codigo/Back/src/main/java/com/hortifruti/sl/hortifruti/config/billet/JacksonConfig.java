package com.hortifruti.sl.hortifruti.config.billet;

import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();

    // Aumenta o limite de profundidade
    objectMapper
        .getFactory()
        .setStreamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(2000).build());

    // Registra o módulo de tempo do Java 8+ (JSR-310)
    objectMapper.registerModule(new JavaTimeModule());

    // Configura para escrever datas como timestamps ISO-8601 em vez de arrays
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    return objectMapper;
  }
}
